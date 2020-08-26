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
import java.time.Duration
import java.time.Instant

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
        cacheConfig: CacheConfig,
        cacheDao: CacheDao,
        time: () -> Instant
) {

    private val app: Javalin

    init {
        val syncManager = SourceManager(sourceFactory)
        val usersManager = UsersManager(usersDao, playersDao, cacheConfig, syncManager, cacheDao, time)
        val gamesManager = GamesManager(playersDao, gamesDao, gameLibraryDao, achievementsDao, achievementStatusDao, syncManager, cacheConfig, cacheDao, time)
        val authManager = AuthManager(authorizationDao, usersManager)

        app = Javalin.create {
            it.accessManager(authManager)
            it.enableCorsForAllOrigins()
        }

        JavalinValidation.register(Platform::class.java, Platform::valueOf)

        app.get("/health", { it.result("ok") }, SecurityUtil.roles(CheetosRole.Public))

        SteamAuthController(steamSource, authManager).register(app)
        OpenXblAuthController(System.getenv("OPENXBL_PUBLIC_APP_KEY"), authManager).register(app)
        GamesControllerV1(gamesManager).register(app)
        UsersControllerV1(app, usersManager)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val steamKey = System.getenv("STEAM_API_KEY")
            val steamSource = SteamSource(steamKey)
            val sourceFactory = SourceFactoryImpl(steamKey)
            val dynamoDb = AmazonDynamoDBClientBuilder.defaultClient()

            val gamesDao = GamesDao("cheetosbros-prod-Games-H2CNV8YSZW3R", dynamoDb)
            val achievementsDao = AchievementsDao("cheetosbros-prod-Achievements-TZRR78IUS2KB", dynamoDb)
            val gameLibraryDao = GameLibraryDao("cheetosbros-prod-GameLibrary-13FP09QLVHHLX", dynamoDb)
            val userAchievementsDao = AchievementStatusDao("cheetosbros-prod-AchievementStatus-CLHMN2Y6WWPK", dynamoDb)
            val playersDao = PlayersDao("cheetosbros-prod-Players-VZB7N9XIRKZ8", dynamoDb)
            val usersDao = UsersDao("cheetosbros-prod-Users-1IWF8EPSH6C4Y", dynamoDb)
            val cacheDao = CacheDao("cheetosbros-prod-Cache-W59C2X4KRF09", dynamoDb)
            val authorizationDao = JwtAuthorizationDao(
                    issuer = "cheetosbros-dev",
                    privateKey = PemUtils.parsePEMFile(Paths.get("../cheetosbros-dev.pem").toUri().toURL())!!,
                    publicKey = PemUtils.parsePEMFile(Paths.get("../cheetosbros-dev-pub.pem").toUri().toURL())!!,
                    playersDao = playersDao
            )

            val cacheConfig = CacheConfig(
                    library = Duration.ofDays(1),
                    achievements = Duration.ofDays(30),
                    achievementStatuses = Duration.ofHours(1),
                    friends = Duration.ofDays(1)
            )

            val time = { Instant.now() }

            val server = ApiServer(
                    authorizationDao, gamesDao, achievementsDao, gameLibraryDao, userAchievementsDao, usersDao,
                    playersDao, sourceFactory, steamSource, cacheConfig, cacheDao, time
            )
            server.app.start(8000)
        }
    }
}