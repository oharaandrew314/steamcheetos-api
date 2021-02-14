package io.andrewohara.cheetosbros.api

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.andrewohara.cheetosbros.api.auth.AuthManager
import io.andrewohara.cheetosbros.api.auth.CheetosRole
import io.andrewohara.cheetosbros.api.auth.JwtAuthorizationDao
import io.andrewohara.cheetosbros.api.auth.steam.SteamAuthController
import io.andrewohara.cheetosbros.api.games.v1.*
import io.andrewohara.cheetosbros.api.users.PlayersDao
import io.andrewohara.cheetosbros.api.users.UsersDao
import io.andrewohara.cheetosbros.api.users.UsersManager
import io.andrewohara.cheetosbros.lib.PemUtils
import io.andrewohara.cheetosbros.sources.*
import io.andrewohara.cheetosbros.sources.steam.SteamSource
import io.javalin.Javalin
import io.javalin.core.JavalinConfig
import io.javalin.core.security.SecurityUtil
import io.javalin.core.validation.JavalinValidation
import io.javalin.plugin.json.JavalinJackson

class ApiBuilder(
    dynamoDb: AmazonDynamoDB,
    gamesTableName: String, libraryTableName: String, achievementsTableName: String, achievementStatusTableName: String,
    playersTableName: String, usersTableName: String,
    publicKeyIssuer: String, publicKey: String, privateKey: String, steamKey: String
) {

    val steamSource = SteamSource(steamKey)
    val sourceFactory = SourceFactoryImpl(steamKey)

    val gamesDao = GamesDao(gamesTableName, dynamoDb)
    val achievementsDao = AchievementsDao(achievementsTableName, dynamoDb)
    val gameLibraryDao = GameLibraryDao(libraryTableName, dynamoDb)
    val achievementStatusDao = AchievementStatusDao(achievementStatusTableName, dynamoDb)
    val playersDao = PlayersDao(playersTableName, dynamoDb)
    val usersDao = UsersDao(usersTableName, dynamoDb)
    val authorizationDao = JwtAuthorizationDao(
        issuer = publicKeyIssuer,
        privateKey = PemUtils.parsePEMFile(privateKey)!!,
        publicKey = PemUtils.parsePEMFile(publicKey)!!,
        playersDao = playersDao
    )

    private val usersManager = UsersManager(usersDao, playersDao)
    private val authManager = AuthManager(authorizationDao, usersManager)

    fun updateConfig(config: JavalinConfig) {
        config.accessManager(authManager)
        config.enableCorsForAllOrigins()

        JavalinJackson.getObjectMapper().registerModule(JavaTimeModule())
        JavalinValidation.register(Platform::class.java, Platform::valueOf)
    }

    fun registerController(app: Javalin) {
        val syncManager = SourceManager(sourceFactory, gamesDao, gameLibraryDao, achievementsDao, achievementStatusDao)
        val gamesManager = GamesManager(playersDao, gamesDao, gameLibraryDao, achievementsDao, achievementStatusDao)

        app.get("/health", { it.result("ok") }, SecurityUtil.roles(CheetosRole.Public))

        SteamAuthController(steamSource, authManager).register(app)
        GamesControllerV1(gamesManager).register(app)
        SyncApiV1(app, usersManager, syncManager)
    }
}