package io.andrewohara.cheetosbros.api

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import io.andrewohara.cheetosbros.api.auth.AuthManager
import io.andrewohara.cheetosbros.api.auth.AuthorizationDao
import io.andrewohara.cheetosbros.api.auth.JwtAuthorizationDao
import io.andrewohara.cheetosbros.api.auth.PemUtils
import io.andrewohara.cheetosbros.api.auth.openxbl.OpenXblAuthController
import io.andrewohara.cheetosbros.api.auth.steam.SteamAuthController
import io.andrewohara.cheetosbros.api.games.v1.*
import io.andrewohara.cheetosbros.api.users.*
import io.andrewohara.cheetosbros.sources.*
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
        playersDao: PlayersDao,
        sourceFactory: SourceFactory,
        steamSource: Source,
        syncExecutor: SyncExecutor,
        friendsDao: FriendsDao,
) {

    private val app: Javalin

    init {
        val usersManager = UsersManager(usersDao, playersDao, friendsDao)
        val gamesManager = GamesManager(gamesDao, userGamesDao, achievementsDao, achievementStatusDao)
        val sourcesManager = SourcesManager(sourceFactory, gamesDao, achievementsDao, userGamesDao, achievementStatusDao, playersDao, friendsDao)
        val authManager = AuthManager(authorizationDao, usersManager)
        val syncManager = SyncManager(syncExecutor, sourcesManager)

        app = Javalin.create {
            it.accessManager(authManager)
            it.enableCorsForAllOrigins()
        }

        app.get("/health") { it.result("ok") }

        SteamAuthController(steamSource, authManager).register(app)
        OpenXblAuthController(System.getenv("OPENXBL_PUBLIC_APP_KEY"), authManager).register(app)
        GamesControllerV1(gamesManager, syncManager).register(app)
        UsersControllerV1(app, usersManager)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val steamKey = System.getenv("STEAM_API_KEY")
            val steamSource = SteamSource(steamKey)
            val sourceFactory = SourceFactoryImpl(steamKey)
            val dynamoDb = AmazonDynamoDBClientBuilder.defaultClient()

            val syncExecutor = ThreadPoolSyncExecutor()
            val gamesDao = GamesDao("cheetosbros-prod-Games-H2CNV8YSZW3R", dynamoDb)
            val achievementsDao = AchievementsDao("cheetosbros-prod-Achievements-TZRR78IUS2KB", dynamoDb)
            val gameStatusDao = UserGamesDao("cheetosbros-prod-OwnedGames-2YKNB7H0KJRQ", dynamoDb)
            val userAchievementsDao = AchievementStatusDao("cheetosbros-prod-AchievementStatus-QZSUCSVQ0ZQH", dynamoDb)
            val playersDao = PlayersDao("cheetosbros-prod-Players-VZB7N9XIRKZ8", dynamoDb)
            val usersDao = UsersDao("cheetosbros-prod-Users-1IWF8EPSH6C4Y", dynamoDb, playersDao)
            val friendsDao = FriendsDao("cheetosbros-prod-Friends-EST7GZ6W7QNL", dynamoDb)
            val authorizationDao = JwtAuthorizationDao(
                    issuer = "cheetosbros-dev",
                    privateKey = PemUtils.parsePEMFile(Paths.get("C:/Users/ohara/Desktop/cheetosbros-dev.pem").toUri().toURL())!!,
                    publicKey = PemUtils.parsePEMFile(Paths.get("C:/Users/ohara/Desktop/cheetosbros-dev-pub.pem").toUri().toURL())!!
            )


            val server = ApiServer(
                    authorizationDao, gamesDao, achievementsDao, gameStatusDao, userAchievementsDao, usersDao,
                    playersDao, sourceFactory, steamSource, syncExecutor, friendsDao
            )
            server.app.start(8000)
        }
    }
}