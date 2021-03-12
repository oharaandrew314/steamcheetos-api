package io.andrewohara.cheetosbros

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTableMapper
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import io.andrewohara.awsmock.dynamodb.MockAmazonDynamoDB
import io.andrewohara.cheetosbros.api.auth.AuthManager
import io.andrewohara.cheetosbros.api.auth.JwtAuthorizationDao
import io.andrewohara.cheetosbros.api.games.*
import io.andrewohara.cheetosbros.lib.PemUtils
import io.andrewohara.cheetosbros.api.users.*
import io.andrewohara.cheetosbros.api.v1.GamesApiV1
import io.andrewohara.cheetosbros.sources.*
import org.junit.rules.ExternalResource
import spark.Spark
import java.time.Duration
import java.time.Instant
import java.util.*

object TestDriver: ExternalResource() {

    val time: Instant = Instant.parse("2021-03-04T01:00:00Z")

    private val dynamoDb = MockAmazonDynamoDB()

    private val socialLinkDao = SocialLinkDao(dynamoDb, "social-links").apply {
        mapper.createTable(ProvisionedThroughput(1, 1))
    }

    val usersDao = UsersDao("users", dynamoDb).apply {
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

    val jobsDao = JobsDao(dynamoDb, "jobs").apply {
        tableMapper.createTable(ProvisionedThroughput(1, 1))
    }

    val authorizationDao = JwtAuthorizationDao(
            issuer = "cheetosbros-test",
            privateKey = PemUtils.parsePEMFile(javaClass.classLoader.getResource("auth/cheetosbros-test.pem")!!)!!,
            publicKey = PemUtils.parsePEMFile(javaClass.classLoader.getResource("auth/cheetosbros-test-pub.pem")!!)!!
    )

    private val steamSource = FakeSource(Platform.Steam)
    private val xboxSource = FakeSource(Platform.Xbox)

    private val sourceFactory = FakeSourceFactory(
        steamSource = steamSource,
        xboxSource = xboxSource
    )

    val sourceManager = SourceManager(
        sourceFactory = sourceFactory,
        gamesDao = gamesDao,
        gameLibraryDao = libraryDao,
        achievementsDao = achievementsDao,
        achievementStatusDao = achievementStatusDao,
        gameCacheDuration = Duration.ofSeconds(30),
        usersDao = usersDao
    )

    val jobService = JobService(sourceManager, jobsDao)
    val gamesManager = GamesManager(gamesDao, libraryDao, achievementsDao, achievementStatusDao)
    private val authManager = AuthManager(authorizationDao, usersDao, socialLinkDao)

    private fun Platform.source() = when(this) {
        Platform.Steam -> steamSource
        Platform.Xbox -> xboxSource
    }

    override fun before() {
        GamesApiV1(gamesManager, jobService) { time }
        Spark.before(authManager)
        Spark.awaitInitialization()
    }

    override fun after() {
        fun <T> DynamoDBTableMapper<T, *, *>.clear() {
            val items = scan(DynamoDBScanExpression()).toList()
            batchDelete(items)
        }

        socialLinkDao.mapper.clear()
        usersDao.mapper.clear()
        gamesDao.mapper.clear()
        achievementsDao.mapper.clear()
        libraryDao.mapper.clear()
        achievementStatusDao.mapper.clear()
        jobsDao.tableMapper.clear()

        steamSource.clear()
        xboxSource.clear()

        Spark.awaitStop()
    }

    fun saveGame(platform: Platform, achievements: Int, name: String? = null): CachedGame {
        val id =  UUID.randomUUID().toString()
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

    fun saveGame(data: GameData): CachedGame {
        gamesDao.create(data)
        return gamesDao[data.uid]!!
    }

    fun saveAchievement(game: CachedGame, name: String? = null, description: String? = null, hidden: Boolean = false, score: Int? = null): Achievement {
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

    fun saveProgress(player: Player, game: CachedGame, achievement: Achievement, unlocked: Instant?): AchievementDetails {
        val status = AchievementStatus(
                achievementId = achievement.id,
                unlockedOn = unlocked
        )

        achievementStatusDao.batchSave(player.uid, game.uid, listOf(status))

        return AchievementDetails.create(achievement, unlocked)
    }

    fun saveToLibrary(player: Player, game: CachedGame, achievements: Int = 0): OwnedGameDetails {
        val ownedGame = OwnedGame(
            uid = game.uid,
            achievements = achievements,
            lastUpdated = time
        )
        libraryDao.save(player, ownedGame)
        return OwnedGameDetails(game, ownedGame)
    }

    fun saveUser(xbox: Boolean = false, steam: Boolean = false): User {
        fun createPlayer(platform: Platform): Player {
            val id = UUID.randomUUID().toString()
            return Player(
                uid = Uid(platform, id),
                avatar = null,
                username = "player-$id-$platform",
                token = null
            )
        }

        val players = mutableMapOf<Platform, Player>()
        if (xbox) players[Platform.Xbox] = createPlayer(Platform.Xbox)
        if (steam) players[Platform.Steam] = createPlayer(Platform.Steam)

        val user = User(UUID.randomUUID(), players)
        usersDao.save(user)
        return user
    }

    // sources

    fun source(player: Player) = player.uid.platform.source()

    fun createPlayer(platform: Platform, displayName: String? = null): Player {
        val id = UUID.randomUUID().toString()
        val player = Player(
            uid = Uid(platform, id),
            avatar = null,
            username = displayName ?: "player-$id",
            token = null
        )
        platform.source().addPlayer(player)
        return player
    }

    fun createGame(platform: Platform, name: String? = null): GameData {
        val id = UUID.randomUUID().toString()
        val game = GameData(
            uid = Uid(platform, id),
            displayImage = null,
            name = name ?: "game-$id",
        )
        platform.source().addGame(game)

        return game
    }

    fun createAchievement(gameData: GameData, name: String? = null, description: String? = null, hidden: Boolean = false, score: Int? = null): Achievement {
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
        gameData.uid.platform.source().addAchievement(gameData.uid.id, achievement)

        return achievement
    }

    fun unlockAchievement(player: Player, gameData: GameData, achievement: Achievement, unlocked: Instant?): AchievementStatus {
        val status = AchievementStatus(
            achievementId = achievement.id,
            unlockedOn = unlocked
        )

        gameData.uid.platform.source().addUserAchievement(gameData.uid.id, player.uid.id, status)

        return status
    }

    fun addToLibrary(player: Player, gameData: GameData) {
        player.uid.platform.source().addGameToLibrary(player.uid.id, gameData.uid.id)
    }
}