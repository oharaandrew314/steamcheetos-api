package io.andrewohara.cheetosbros.api

import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import io.andrewohara.awsmock.dynamodb.MockAmazonDynamoDB
import io.andrewohara.awsmock.sqs.MockAmazonSQS
import io.andrewohara.cheetosbros.api.games.v1.*
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.api.v1.GamesApiV1
import io.andrewohara.cheetosbros.sources.Platform
import io.andrewohara.cheetosbros.sources.Player
import io.andrewohara.cheetosbros.sync.SqsSyncClient
import org.junit.rules.ExternalResource
import spark.Filter
import spark.Request
import spark.Response
import spark.Spark

class TestDriver: ExternalResource() {

    lateinit var gamesDao: GamesDao
    lateinit var libraryDao: GameLibraryDao
    private lateinit var achievementsDao: AchievementsDao
    private lateinit var progressDao: AchievementStatusDao

    val steamPlayer1 = Player(
        platform = Platform.Steam,
        id = "player1",
        avatar = null,
        username = "player one",
        token = null
    )

    var currentUser = User(
        id = "user1",
        players = mapOf(Platform.Steam to steamPlayer1)
    )

    override fun before() {
        val dynamoDb = MockAmazonDynamoDB()

        gamesDao = GamesDao("games", dynamoDb).apply {
            mapper.createTable(ProvisionedThroughput(1, 1))
        }
        achievementsDao = AchievementsDao("achievements", dynamoDb).apply {
            mapper.createTable(ProvisionedThroughput(1, 1))
        }
        libraryDao = GameLibraryDao("library", dynamoDb).apply {
            mapper.createTable(ProvisionedThroughput(1, 1))
        }
        progressDao = AchievementStatusDao("progress", dynamoDb).apply {
            mapper.createTable(ProvisionedThroughput(1, 1))
        }

        val sqs = MockAmazonSQS()
        sqs.createQueue("sync-queue")

        GamesApiV1(
            gamesManager = GamesManager(gamesDao, libraryDao, achievementsDao, progressDao),
            syncClient = SqsSyncClient(sqs, "sync-queue")
        )
        Spark.before(object: Filter {
            override fun handle(request: Request, response: Response) {
                request.attribute("user", currentUser)
            }
        })

        Spark.awaitInitialization()
    }

    override fun after() {
        Spark.awaitStop()
    }
}