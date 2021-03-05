package io.andrewohara.cheetosbros.api

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTableMapper
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import io.andrewohara.awsmock.dynamodb.MockAmazonDynamoDB
import io.andrewohara.awsmock.sqs.MockAmazonSQS
import io.andrewohara.cheetosbros.api.auth.AuthManager
import io.andrewohara.cheetosbros.api.auth.JwtAuthorizationDao
import io.andrewohara.cheetosbros.api.games.*
import io.andrewohara.cheetosbros.lib.PemUtils
import io.andrewohara.cheetosbros.api.users.*
import io.andrewohara.cheetosbros.api.v1.GamesApiV1
import io.andrewohara.cheetosbros.sources.*
import io.andrewohara.cheetosbros.sync.SqsSyncClient
import org.junit.rules.ExternalResource
import spark.Spark
import java.time.Instant
import java.util.*

object ApiTestDriver: ExternalResource() {

    val time: Instant = Instant.parse("2021-03-04T01:00:00Z")

    // Test Fixtures
    val steamPlayer1 = Player(
        uid = Uid(Platform.Steam, "player1"),
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
                uid = Uid(platform, id),
                avatar = null,
                username = displayName ?: "player-$id",
                token = null
        )
    }

    fun createGame(platform: Platform, achievements: Int, name: String? = null): CachedGame {
        val id = UUID.randomUUID().toString()
        val game = CachedGame(
                uid = Uid(platform, id),
                displayImage = null,
                name = name ?: "game-$id",
                achievements = achievements,
                lastUpdated = time
        )
        gamesDao.save(game)

        return game
    }

    fun createAchievement(game: CachedGame, name: String? = null, description: String? = null, hidden: Boolean = false, score: Int? = null): Achievement {
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

        achievementsDao.batchSave(game.uid, listOf(achievement))

        return achievement
    }

    fun unlockAchievement(player: Player, game: CachedGame, achievement: Achievement, unlocked: Instant?): AchievementDetails {
        val status = AchievementStatus(
                achievementId = achievement.id,
                unlockedOn = unlocked
        )

        achievementStatusDao.batchSave(player.uid, game.uid, listOf(status))

        return AchievementDetails.create(achievement, unlocked)
    }

    fun addToLibrary(player: Player, game: CachedGame, achievements: Int = 0): OwnedGameDetails {
        val ownedGame = OwnedGame(
            uid = game.uid,
            achievements = achievements,
            lastUpdated = time
        )
        libraryDao.save(player, ownedGame)
        return OwnedGameDetails(game, ownedGame)
    }

    fun createUser(xbox: Player? = null, steam: Player? = null): User {
        val players = mutableMapOf<Platform, Player>()
        if (xbox != null) players[xbox.uid.platform] = xbox
        if (steam != null) players[steam.uid.platform] = steam

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