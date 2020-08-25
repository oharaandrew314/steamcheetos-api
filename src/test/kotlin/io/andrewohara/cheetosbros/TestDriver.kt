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
    lateinit var authorizationDao: AuthorizationDao

    // Sources
    private lateinit var steamSource: FakeSource
    private lateinit var xboxSource: FakeSource
    private lateinit var sourceFactory: SourceFactory

    // Managers
    lateinit var gamesManager: GamesManager
    lateinit var syncManager: SyncManager
    lateinit var usersManager: UsersManager

    // Test Helpers
    val sourceHelper = SourceHelper()
    val gamesHelper = GamesHelper()

    // Config
    lateinit var time: Instant
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

        authorizationDao = JwtAuthorizationDao(
                issuer = "cheetosbros-test",
                privateKey = PemUtils.parsePEMFile(javaClass.classLoader.getResource("auth/cheetosbros-test.pem")!!)!!,
                publicKey = PemUtils.parsePEMFile(javaClass.classLoader.getResource("auth/cheetosbros-test-pub.pem")!!)!!,
                playersDao = playersDao
        )

        steamSource = FakeSource(Platform.Steam)
        xboxSource = FakeSource(Platform.Xbox)
        sourceFactory = FakeSourceFactory(steamSource = steamSource, xboxSource = xboxSource)

        syncManager = SyncManager(sourceFactory, gamesDao, achievementsDao, gameLibraryDao, achievementStatusDao, playersDao) { time }
        gamesManager = GamesManager(playersDao, gamesDao, gameLibraryDao, achievementsDao, achievementStatusDao, syncManager, cacheConfig) { time }
        usersManager = UsersManager(usersDao, playersDao, cacheConfig, syncManager) { time }
    }

    inner class SourceHelper {
        fun createUser(displayName: String? = null, vararg players: Player): User {
            val id = UUID.randomUUID().toString()
            val user = User(
                    id = id,
                    displayName = displayName ?: "user-$id",
                    openxblToken = if (players.any { it.platform == Platform.Xbox }) "token" else null
            )
            usersDao.save(user)
            for (player in players) {
                playersDao.linkUser(player, user)
            }

            return user
        }

        fun createPlayer(platform: Platform = Platform.Steam, displayName: String? = null): Player {
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

        fun createGame(platform: Platform = Platform.Steam, name: String? = null): Game {
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

        fun addToLibrary(game: Game, player: Player) {
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
    }

    inner class GamesHelper {
        private var idCounter = 1
        private var time: Instant = Instant.parse("2020-01-01T00:00:00Z")

        private fun nextTime(): Instant {
            val result = time
            time += Duration.ofHours(1)
            return result
        }

        fun createPlayer(platform: Platform, username: String? = null, avatar: String? = null): Player {
            val id = idCounter++.toString()
            val player = Player(id = id, username = username ?: "user $id", platform = platform, avatar = avatar)
            playersDao.save(player)
            return player
        }

        fun createGame(platform: Platform, name: String? = null, displayImage: String? = null): Game {
            val id = idCounter++.toString()
            val game = Game(platform = platform, id = id, name = name ?: "game $id", displayImage = displayImage)
            gamesDao.save(game)

            return game
        }

        fun createAchievement(game: Game, name: String? = null, description: String? = null, score: Int? = null, hidden: Boolean = false, icons: List<String> = emptyList()): Achievement {
            val id = idCounter++.toString()
            val achievement = Achievement(id = id, name = name ?: "Achievement $id", description = description, hidden = hidden , score = score, icons = icons)
            achievementsDao.batchSave(game, listOf(achievement))

            return achievement
        }

        fun unlock(player: Player, game: Game, achievement: Achievement, date: Instant? = null): AchievementStatus {
            val status = AchievementStatus(achievementId = achievement.id, unlockedOn = date ?: nextTime())
            achievementStatusDao.batchSave(player, game, listOf(status))

            return status
        }

        fun addToLibrary(player: Player, game: Game): LibraryItem {
            val item = LibraryItem(platform = game.platform, gameId = game.id)
            gameLibraryDao.batchSave(player, listOf(item))

            return item
        }

        fun createUser(displayName: String? = null, vararg players: Player): User {
            val id = idCounter++.toString()
            val user = User(id = id, displayName = displayName ?: "user $id")
            usersDao.save(user)

            for (player in players) {
                playersDao.linkUser(player, user)
            }

            return user
        }

        fun addFriend(player: Player, friend: Player) {
            val friends = playersDao.getFriends(player)?.toSet() ?: emptySet()
            playersDao.saveFriends(player, friends.map { it.id } + friend.id)
        }
    }
}