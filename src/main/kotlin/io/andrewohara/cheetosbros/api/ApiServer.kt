package io.andrewohara.cheetosbros.api

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import io.andrewohara.cheetosbros.api.auth.AuthManager
import io.andrewohara.cheetosbros.api.auth.AuthorizationDao
import io.andrewohara.cheetosbros.api.auth.JwtAuthorizationDao
import io.andrewohara.cheetosbros.api.auth.PemUtils
import io.andrewohara.cheetosbros.api.auth.openxbl.OpenXblAuthController
import io.andrewohara.cheetosbros.api.auth.steam.SteamAuthController
import io.andrewohara.cheetosbros.api.games.v1.*
import io.andrewohara.cheetosbros.api.users.UsersDao
import io.andrewohara.cheetosbros.api.users.UsersManager
import io.andrewohara.cheetosbros.sources.LocalSyncExecutor
import io.andrewohara.cheetosbros.sources.Source
import io.andrewohara.cheetosbros.sources.SourcesManager
import io.andrewohara.cheetosbros.sources.steam.SteamSource
import io.javalin.Javalin
import java.nio.file.Paths

class ApiServer(
        authorizationDao: AuthorizationDao,
        gamesDao: GamesDao,
        achievementsDao: AchievementsDao,
        userGamesDao: UserGamesDao,
        achievementStatusDao: AchievementStatusDao,
        usersDao: UsersDao,
        steamSource: Source
) {

    private val app: Javalin

    init {
        val usersManager = UsersManager(usersDao)
        val gamesManager = GamesManager(gamesDao, userGamesDao, achievementsDao, achievementStatusDao)
        val sourcesManager = SourcesManager(steamSource, gamesDao, achievementsDao, userGamesDao, achievementStatusDao)
        val syncExecutor = LocalSyncExecutor(sourcesManager)
        val authManager = AuthManager(authorizationDao, usersManager)

        app = Javalin.create {
            it.accessManager(authManager)
            it.enableCorsForAllOrigins()
        }

        app.get("/health") { it.result("ok") }

        SteamAuthController(steamSource, authManager).register(app)
        OpenXblAuthController(System.getenv("OPENXBL_PUBLIC_APP_KEY"), authManager).register(app)
        GamesControllerV1(gamesManager, syncExecutor).register(app)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val steamSource = SteamSource(System.getenv("STEAM_API_KEY"))
            val dynamoDb = AmazonDynamoDBClientBuilder.defaultClient()

            val gamesDao = GamesDao("cheetosbros-games-dev", dynamoDb)
            val achievementsDao = AchievementsDao("cheetosbros-achievements-dev", dynamoDb)
            val gameStatusDao = UserGamesDao("cheetosbros-gamestatus-dev", dynamoDb)
            val userAchievementsDao = AchievementStatusDao("cheetosbros-user-achievements-dev", dynamoDb)
            val usersDao = UsersDao("cheetosbros-users-dev", dynamoDb)
            val authorizationDao = JwtAuthorizationDao(
                    issuer = "cheetosbros-dev",
                    privateKey = PemUtils.parsePEMFile(Paths.get("C:/Users/ohara/Desktop/cheetosbros-dev.pem").toUri().toURL())!!,
                    publicKey = PemUtils.parsePEMFile(Paths.get("C:/Users/ohara/Desktop/cheetosbros-dev-pub.pem").toUri().toURL())!!
            )

            val server = ApiServer(authorizationDao, gamesDao, achievementsDao, gameStatusDao, userAchievementsDao, usersDao, steamSource)
            server.app.start(8000)
        }
    }
}