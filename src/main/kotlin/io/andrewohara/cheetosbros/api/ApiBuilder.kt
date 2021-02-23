package io.andrewohara.cheetosbros.api

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import io.andrewohara.cheetosbros.api.auth.AuthManager
import io.andrewohara.cheetosbros.api.auth.JwtAuthorizationDao
import io.andrewohara.cheetosbros.api.auth.SteamOpenID
import io.andrewohara.cheetosbros.api.games.v1.*
import io.andrewohara.cheetosbros.api.users.DynamoSocialLinkDao
import io.andrewohara.cheetosbros.api.users.UsersDao
import io.andrewohara.cheetosbros.lib.PemUtils
import io.andrewohara.cheetosbros.sources.*
import io.andrewohara.cheetosbros.sources.steam.SteamSource
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

    fun startSpark(port: Int? = null, cors: Boolean = false, decodeQueryParams: Boolean) {
        if (port != null) {
            Spark.port(port)
        }

        SparkApi(gamesManager, authManager, steamOpenId, syncManager, frontendHost, decodeQueryParams)

        if (cors) {
            Spark.after(SparkCorsFilter(frontendHost))
        }

        Spark.awaitInitialization()
    }
}