package io.andrewohara.cheetosbros

import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import io.andrewohara.awsmock.dynamodb.MockAmazonDynamoDB
import io.andrewohara.cheetosbros.api.CacheConfig
import io.andrewohara.cheetosbros.api.auth.AuthorizationDao
import io.andrewohara.cheetosbros.api.auth.JwtAuthorizationDao
import io.andrewohara.cheetosbros.api.auth.PemUtils
import io.andrewohara.cheetosbros.api.games.v1.*
import io.andrewohara.cheetosbros.api.users.*
import io.andrewohara.cheetosbros.sources.*
import org.junit.rules.ExternalResource
import java.time.Duration
import java.time.Instant
import java.util.*

class TestDriver: ExternalResource() {

    // Daos
    private lateinit var playersDao: PlayersDao
    private lateinit var usersDao: UsersDao
    private lateinit var gamesDao: GamesDao
    private lateinit var achievementsDao: AchievementsDao
    private lateinit var gameLibraryDao: GameLibraryDao
    private lateinit var achievementStatusDao: AchievementStatusDao
    private lateinit var cacheDao: CacheDao
    lateinit var authorizationDao: AuthorizationDao

    // Sources
    private lateinit var steamSource: FakeSource
    private lateinit var xboxSource: FakeSource
    private lateinit var sourceFactory: SourceFactory

    // Managers
    lateinit var gamesManager: GamesManager
    lateinit var sourceManager: SourceManager
    lateinit var usersManager: UsersManager

    // Config
    private lateinit var time: Instant
    var cacheConfig = CacheConfig(
            library = Duration.ofDays(1),
            achievements = Duration.ofDays(30),
            achievementStatuses = Duration.ofHours(1),
            friends = Duration.ofDays(1)
    )

    override fun before() {
        val dynamoDb = MockAmazonDynamoDB()

        time = Instant.parse("2020-01-01T00:00:00Z")

        playersDao = PlayersDao("players", dynamoDb)
        playersDao.mapper.createTable(ProvisionedThroughput(1, 1))

        usersDao = UsersDao("users", dynamoDb)
        usersDao.mapper.createTable(ProvisionedThroughput(1, 1))

        gamesDao = GamesDao("games", dynamoDb)
        gamesDao.mapper.createTable(ProvisionedThroughput(1, 1))

        achievementsDao = AchievementsDao("achievements", dynamoDb)
        achievementsDao.mapper.createTable(ProvisionedThroughput(1, 1))

        gameLibraryDao = GameLibraryDao("user-games", dynamoDb)
        gameLibraryDao.mapper.createTable(ProvisionedThroughput(1, 1))

        achievementStatusDao = AchievementStatusDao("user-achievements", dynamoDb)
        achievementStatusDao.mapper.createTable(ProvisionedThroughput(1, 1))

        cacheDao = CacheDao("cache", dynamoDb)
        cacheDao.mapper.createTable(ProvisionedThroughput(1, 1))

        authorizationDao = JwtAuthorizationDao(
                issuer = "cheetosbros-test",
                privateKey = PemUtils.parsePEMFile(javaClass.classLoader.getResource("auth/cheetosbros-test.pem")!!)!!,
                publicKey = PemUtils.parsePEMFile(javaClass.classLoader.getResource("auth/cheetosbros-test-pub.pem")!!)!!,
                playersDao = playersDao
        )

        steamSource = FakeSource(Platform.Steam)
        xboxSource = FakeSource(Platform.Xbox)
        sourceFactory = FakeSourceFactory(steamSource = steamSource, xboxSource = xboxSource)

        sourceManager = SourceManager(sourceFactory)
        gamesManager = GamesManager(playersDao, gamesDao, gameLibraryDao, achievementsDao, achievementStatusDao, sourceManager, cacheConfig, cacheDao) { time }
        usersManager = UsersManager(usersDao, playersDao, cacheConfig, sourceManager, cacheDao) { time }
    }

    fun createPlayer(platform: Platform, displayName: String? = null): Player {
        val id = UUID.randomUUID().toString()
        val player = Player(
                id = id,
                avatar = null,
                platform = platform,
                username = displayName ?: "player-$id"
        )
        platform.source().addPlayer(player)
        return player
    }

    fun createGame(platform: Platform, name: String? = null): Game {
        val id = UUID.randomUUID().toString()
        val game = Game(
                id = id,
                displayImage = null,
                name = name ?: "game-$id",
                platform = platform
        )
        platform.source().addGame(game)

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

        game.platform.source().addAchievement(game.id, achievement)

        return achievement
    }

    fun unlockAchievement(player: Player, game: Game, achievement: Achievement, unlocked: Instant?): AchievementStatus {
        val status = AchievementStatus(
                achievementId = achievement.id,
                unlockedOn = unlocked
        )

        game.platform.source().addUserAchievement(game.id, player.id, status)

        return status
    }

    fun addToLibrary(player: Player, game: Game) {
        val source = player.platform.source()

        source.addGameToLibrary(player.id, game.id)
    }

    fun addFriend(player: Player, friend: Player) {
        val source = player.platform.source()
        source.addFriend(userId = player.id, friendId = friend.id)
    }

    fun removeFriend(player: Player, friend: Player) {
        val source = player.platform.source()
        source.removeFriend(userId = player.id, friendId = friend.id)
    }

    private fun Platform.source() = when(this) {
        Platform.Steam -> steamSource
        Platform.Xbox -> xboxSource
    }

    fun createUser(displayName: String? = null, xbox: Player? = null, steam: Player? = null): User {
        val id = UUID.randomUUID().toString()
        val user = User(
                id = id,
                displayName = displayName ?: "user-$id",
                openxblToken = if (xbox != null) "token" else null
        )
        usersDao.save(user)

        if (xbox != null) playersDao.linkUser(xbox, user)
        if (steam != null) playersDao.linkUser(steam, user)

        return user
    }
}