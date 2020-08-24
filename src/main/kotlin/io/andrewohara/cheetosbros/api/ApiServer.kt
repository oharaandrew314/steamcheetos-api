package io.andrewohara.cheetosbros.api

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import io.andrewohara.cheetosbros.api.auth.*
import io.andrewohara.cheetosbros.api.auth.openxbl.OpenXblAuthController
import io.andrewohara.cheetosbros.api.auth.steam.SteamAuthController
import io.andrewohara.cheetosbros.api.games.v1.*
import io.andrewohara.cheetosbros.api.users.*
import io.andrewohara.cheetosbros.sources.*
import io.andrewohara.cheetosbros.sources.steam.SteamSource
import io.javalin.Javalin
import io.javalin.core.security.SecurityUtil
import io.javalin.core.validation.JavalinValidation
import java.nio.file.Paths

class ApiServer(
        authorizationDao: AuthorizationDao,
        gamesDao: GamesDao,
        achievementsDao: AchievementsDao,
        gameLibraryDao: GameLibraryDao,
        achievementStatusDao: AchievementStatusDao,
        usersDao: UsersDao,
        playersDao: PlayersDao,
        sourceFactory: SourceFactory,
        steamSource: Source,
        syncExecutor: SyncExecutor
) {

    private val app: Javalin

    init {
        val usersManager = UsersManager(usersDao, playersDao)
        val gamesManager = GamesManager(playersDao, gamesDao, gameLibraryDao, achievementsDao, achievementStatusDao)
        val syncManager = SyncManager(sourceFactory, syncExecutor, gamesDao, achievementsDao, gameLibraryDao, achievementStatusDao, playersDao)
        val authManager = AuthManager(authorizationDao, usersManager)

        app = Javalin.create {
            it.accessManager(authManager)
            it.enableCorsForAllOrigins()
        }

        JavalinValidation.register(Platform::class.java, Platform::valueOf)

        app.get("/health", { it.result("ok") }, SecurityUtil.roles(CheetosRole.Public))

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
            val gameLibraryDao = GameLibraryDao("cheetosbros-prod-GameLibrary-13FP09QLVHHLX", dynamoDb)
            val userAchievementsDao = AchievementStatusDao("cheetosbros-prod-AchievementStatus-CLHMN2Y6WWPK", dynamoDb)
            val playersDao = PlayersDao("cheetosbros-prod-Players-VZB7N9XIRKZ8", dynamoDb)
            val usersDao = UsersDao("cheetosbros-prod-Users-1IWF8EPSH6C4Y", dynamoDb)
            val authorizationDao = JwtAuthorizationDao(
                    issuer = "cheetosbros-dev",
                    privateKey = PemUtils.parsePEMFile(Paths.get("C:/Users/ohara/Desktop/cheetosbros-dev.pem").toUri().toURL())!!,
                    publicKey = PemUtils.parsePEMFile(Paths.get("C:/Users/ohara/Desktop/cheetosbros-dev-pub.pem").toUri().toURL())!!,
                    playersDao = playersDao
            )


            val server = ApiServer(
                    authorizationDao, gamesDao, achievementsDao, gameLibraryDao, userAchievementsDao, usersDao,
                    playersDao, sourceFactory, steamSource, syncExecutor
            )
            server.app.start(8000)
        }
    }
}