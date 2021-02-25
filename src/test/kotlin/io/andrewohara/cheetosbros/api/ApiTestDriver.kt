package io.andrewohara.cheetosbros.api

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTableMapper
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import io.andrewohara.awsmock.dynamodb.MockAmazonDynamoDB
import io.andrewohara.awsmock.sqs.MockAmazonSQS
import io.andrewohara.cheetosbros.api.auth.AuthManager
import io.andrewohara.cheetosbros.api.auth.JwtAuthorizationDao
import io.andrewohara.cheetosbros.lib.PemUtils
import io.andrewohara.cheetosbros.api.games.v1.*
import io.andrewohara.cheetosbros.api.users.*
import io.andrewohara.cheetosbros.api.v1.GamesApiV1
import io.andrewohara.cheetosbros.sources.*
import io.andrewohara.cheetosbros.sync.SqsSyncClient
import org.junit.rules.ExternalResource
import spark.Spark
import java.time.Instant
import java.util.*

object ApiTestDriver: ExternalResource() {

    // Test Fixtures
    val steamPlayer1 = Player(
        platform = Platform.Steam,
        id = "player1",
        avatar = null,
        username = "player one",
        token = null
    )

    private const val syncQueueName = "sync-queue"

    private val dynamoDb = MockAmazonDynamoDB()
    private val sqs = MockAmazonSQS()

    private val socialLinkDao = SocialLinkDao(dynamoDb, "social-links").apply {
        mapper.createTable(ProvisionedThroughput(1, 1))
    }

    private val usersDao = UsersDao("users", dynamoDb).apply {
        mapper.createTable(ProvisionedThroughput(1, 1))
    }

    val gamesDao = GamesDao("games", dynamoDb).apply {
        mapper.createTable(ProvisionedThroughput(1, 1))
    }

    val achievementsDao = AchievementsDao("achievements", dynamoDb).apply {
        mapper.createTable(ProvisionedThroughput(1, 1))
    }

    val libraryDao = GameLibraryDao("user-games", dynamoDb).apply {
        mapper.createTable(ProvisionedThroughput(1, 1))
    }

    val achievementStatusDao = AchievementStatusDao("user-achievements", dynamoDb).apply {
        mapper.createTable(ProvisionedThroughput(1, 1))
    }

    val authorizationDao = JwtAuthorizationDao(
            issuer = "cheetosbros-test",
            privateKey = PemUtils.parsePEMFile(javaClass.classLoader.getResource("auth/cheetosbros-test.pem")!!)!!,
            publicKey = PemUtils.parsePEMFile(javaClass.classLoader.getResource("auth/cheetosbros-test-pub.pem")!!)!!
    )

    private val syncQueueUrl = sqs.createQueue(syncQueueName).queueUrl

    val gamesManager = GamesManager(gamesDao, libraryDao, achievementsDao, achievementStatusDao)
    private val authManager = AuthManager(authorizationDao, usersDao, socialLinkDao)

    override fun before() {
        GamesApiV1(gamesManager = gamesManager, syncClient = SqsSyncClient(sqs, syncQueueName))
        Spark.before(authManager)
        Spark.awaitInitialization()
    }

    override fun after() {
        socialLinkDao.mapper.clear()
        usersDao.mapper.clear()
        gamesDao.mapper.clear()
        achievementsDao.mapper.clear()
        libraryDao.mapper.clear()
        achievementStatusDao.mapper.clear()
        sqs[syncQueueUrl]!!.messages.clear()

        Spark.awaitStop()
    }

    fun createPlayer(platform: Platform, displayName: String? = null): Player {
        val id = UUID.randomUUID().toString()
        return Player(
                id = id,
                avatar = null,
                platform = platform,
                username = displayName ?: "player-$id",
                token = null
        )
    }

    fun createGame(platform: Platform, name: String? = null): Game {
        val id = UUID.randomUUID().toString()
        val game = Game(
                id = id,
                displayImage = null,
                name = name ?: "game-$id",
                platform = platform
        )
        gamesDao.save(game)

        return game
    }

    fun createAchievement(game: Game, name: String? = null, description: String? = null, hidden: Boolean = false, score: Int? = null): Achievement {
        val id = UUID.randomUUID().toString()
        val actualName = name ?: "achievement-$id"
        val achievement = Achievement(
                id = id,
                name = actualName,
                description = description ?: "description for $actualName",
                hidden = hidden,
                icons = emptyList(),
                score = score
        )

        achievementsDao.batchSave(game, listOf(achievement))

        return achievement
    }

    fun unlockAchievement(player: Player, game: Game, achievement: Achievement, unlocked: Instant?): AchievementStatus {
        val status = AchievementStatus(
                achievementId = achievement.id,
                unlockedOn = unlocked
        )

        achievementStatusDao.batchSave(player, game, listOf(status))

        return status
    }

    fun addToLibrary(player: Player, game: Game, completed: Int, total: Int): OwnedGame {
        val ownedGame = OwnedGame(
            platform = game.platform,
            id = game.id,
            name = game.name,
            displayImage = game.displayImage,
            currentAchievements = completed,
            totalAchievements = total
        )
        libraryDao.save(player, ownedGame)
        return ownedGame
    }

    fun createUser(xbox: Player? = null, steam: Player? = null): User {
        val players = mutableMapOf<Platform, Player>()
        if (xbox != null) players[xbox.platform] = xbox
        if (steam != null) players[steam.platform] = steam

        val user = User(
                id = UUID.randomUUID().toString(),
                players = players
        )

        usersDao.save(user)
        return user
    }

    private fun <T, H, R> DynamoDBTableMapper<T, H, R>.clear() {
        val items = scan(DynamoDBScanExpression()).toList()
        batchDelete(items)
    }
}