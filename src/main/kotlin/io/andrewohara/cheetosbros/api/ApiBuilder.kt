package io.andrewohara.cheetosbros.api

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.andrewohara.cheetosbros.api.auth.AuthManager
import io.andrewohara.cheetosbros.api.auth.CheetosRole
import io.andrewohara.cheetosbros.api.auth.JwtAuthorizationDao
import io.andrewohara.cheetosbros.api.auth.steam.SteamAuthController
import io.andrewohara.cheetosbros.api.auth.steam.SteamOpenID
import io.andrewohara.cheetosbros.api.games.v1.*
import io.andrewohara.cheetosbros.api.users.DynamoSocialLinkDao
import io.andrewohara.cheetosbros.api.users.UsersDao
import io.andrewohara.cheetosbros.lib.PemUtils
import io.andrewohara.cheetosbros.sources.*
import io.andrewohara.cheetosbros.sources.steam.SteamSource
import io.javalin.Javalin
import io.javalin.core.JavalinConfig
import io.javalin.core.security.SecurityUtil
import io.javalin.core.validation.JavalinValidation
import io.javalin.plugin.json.JavalinJackson
import spark.Spark

class ApiBuilder(
    dynamoDb: AmazonDynamoDB,
    gamesTableName: String, libraryTableName: String, achievementsTableName: String, achievementStatusTableName: String,
    socialLinkTableName: String, usersTableName: String,
    publicKeyIssuer: String, publicKey: String, privateKey: String, steamKey: String,
    private val frontendHost: String
) {

    private val steamSource = SteamSource(steamKey)
    private val sourceFactory = SourceFactoryImpl(steamKey)
    private val gamesDao = GamesDao(gamesTableName, dynamoDb)
    private val achievementsDao = AchievementsDao(achievementsTableName, dynamoDb)
    private val gameLibraryDao = GameLibraryDao(libraryTableName, dynamoDb)
    private val achievementStatusDao = AchievementStatusDao(achievementStatusTableName, dynamoDb)
    private val syncManager = SourceManager(sourceFactory, gamesDao, gameLibraryDao, achievementsDao, achievementStatusDao)

    private val gamesManager = GamesManager(gamesDao, gameLibraryDao, achievementsDao, achievementStatusDao)
    private val steamOpenId = SteamOpenID(steamSource)

    private val authorizationDao = JwtAuthorizationDao(
        issuer = publicKeyIssuer,
        privateKey = PemUtils.parsePEMFile(privateKey)!!,
        publicKey = PemUtils.parsePEMFile(publicKey)!!
    )

    private val authManager = let {
        val usersDao = UsersDao(usersTableName, dynamoDb)
        val socialLinkDao = DynamoSocialLinkDao(dynamoDb, socialLinkTableName)
        AuthManager(authorizationDao, usersDao, socialLinkDao)
    }

    fun updateConfig(config: JavalinConfig) {
        config.accessManager(authManager)
//        config.enableCorsForAllOrigins()

        JavalinJackson.getObjectMapper().registerModule(JavaTimeModule())
        JavalinValidation.register(Platform::class.java, Platform::valueOf)
    }

    fun registerController(app: Javalin) {
        app.get("/health", { it.result("ok") }, SecurityUtil.roles(CheetosRole.Public))

        SteamAuthController(steamSource, authManager).register(app)
        GamesControllerV1(gamesManager).register(app)
        SyncApiV1(app, syncManager)
    }

    fun startSpark(port: Int? = null, cors: Boolean = false) {
        if (port != null) {
            Spark.port(port)
        }

        SparkApi(gamesManager, authManager, steamOpenId, syncManager, frontendHost)

        if (cors) {
            Spark.after(SparkCorsFilter(frontendHost))
        }

        Spark.awaitInitialization()
    }
}