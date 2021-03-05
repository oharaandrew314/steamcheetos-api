package io.andrewohara.cheetosbros.api

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.readText

object DevelopmentApiServer {

    @ExperimentalPathApi
    @JvmStatic
    fun main(args: Array<String>) {
        val builder = ApiBuilder(
            dynamoDb = AmazonDynamoDBClientBuilder.defaultClient(),
            gamesTableName = System.getenv("GAMES_TABLE"),
            achievementsTableName = System.getenv("ACHIEVEMENTS_TABLE"),
            achievementStatusTableName = System.getenv("ACHIEVEMENT_STATUS_TABLE"),
            usersTableName = System.getenv("USERS_TABLE"),
            socialLinkTableName = System.getenv("SOCIAL_LINK_TABLE"),
            libraryTableName = System.getenv("LIBRARY_TABLE"),
            steamKey = System.getenv("STEAM_API_KEY"),
            privateKey = Paths.get(System.getenv("PRIVATE_PEM_PATH")).readText(),
            publicKey = Paths.get(System.getenv("PUBLIC_PEM_PATH")).readText(),
            syncQueueUrl = System.getenv("SYNC_QUEUE_URL"),
            publicKeyIssuer = "cheetosbros-localhost",
            frontendHost = "http://localhost:3000"
        )

        builder.startSpark(8000, cors = true, decodeQueryParams = false)
    }
}