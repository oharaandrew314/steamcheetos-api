package io.andrewohara.cheetosbros.api

import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import io.andrewohara.awsmock.dynamodb.MockAmazonDynamoDB
import io.andrewohara.cheetosbros.api.auth.AuthorizationDao
import io.andrewohara.cheetosbros.api.auth.JwtAuthorizationDao
import io.andrewohara.cheetosbros.lib.PemUtils
import io.andrewohara.cheetosbros.api.games.v1.*
import io.andrewohara.cheetosbros.api.users.*
import io.andrewohara.cheetosbros.sources.*
import org.junit.rules.ExternalResource
import java.time.Instant
import java.util.*

class ApiTestDriver: ExternalResource() {

    // Daos
    private lateinit var playersDao: PlayersDao
    private lateinit var usersDao: UsersDao
    lateinit var gamesDao: GamesDao
    lateinit var achievementsDao: AchievementsDao
    lateinit var gameLibraryDao: GameLibraryDao
    lateinit var achievementStatusDao: AchievementStatusDao
    lateinit var authorizationDao: AuthorizationDao

    // Managers
    lateinit var gamesManager: GamesManager
    lateinit var usersManager: UsersManager

    // Config
    private lateinit var time: Instant

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

        gamesManager = GamesManager(playersDao, gamesDao, gameLibraryDao, achievementsDao, achievementStatusDao)
        usersManager = UsersManager(usersDao, playersDao)
    }

    fun createPlayer(platform: Platform, displayName: String? = null): Player {
        val id = UUID.randomUUID().toString()
        val player = Player(
                id = id,
                avatar = null,
                platform = platform,
                username = displayName ?: "player-$id"
        )
        playersDao.save(player)
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

    fun addToLibrary(player: Player, game: Game) {
        gameLibraryDao.batchSave(player, listOf(LibraryItem(game)))
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