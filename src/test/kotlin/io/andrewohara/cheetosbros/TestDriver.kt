package io.andrewohara.cheetosbros

import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import io.andrewohara.awsmock.dynamodb.MockAmazonDynamoDB
import io.andrewohara.cheetosbros.api.games.v1.*
import io.andrewohara.cheetosbros.api.users.SocialLink
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.api.users.UsersDao
import io.andrewohara.cheetosbros.sources.*
import org.junit.rules.ExternalResource
import java.time.Instant
import java.util.*

class TestDriver: ExternalResource() {

    private lateinit var usersDao: UsersDao
    private lateinit var gamesDao: GamesDao
    private lateinit var achievementsDao: AchievementsDao
    private lateinit var userGamesDao: UserGamesDao
    private lateinit var achievementStatusDao: AchievementStatusDao

    private lateinit var steamSource: FakeSource
    private lateinit var xboxSource: FakeSource

    lateinit var sourcesManager: SourcesManager
    lateinit var gamesManager: GamesManager
    lateinit var syncManager: SyncManager

    private val syncExecutor = InlineSyncExecutor()

    override fun before() {
        val dynamoDb = MockAmazonDynamoDB()

        usersDao = UsersDao("users", dynamoDb)
        usersDao.mapper.createTable(ProvisionedThroughput(1, 1))

        gamesDao = GamesDao("games", dynamoDb)
        gamesDao.mapper.createTable(ProvisionedThroughput(1, 1))

        achievementsDao = AchievementsDao("achievements", dynamoDb)
        achievementsDao.mapper.createTable(ProvisionedThroughput(1, 1))

        userGamesDao = UserGamesDao("user-games", dynamoDb)
        userGamesDao.mapper.createTable(ProvisionedThroughput(1, 1))

        achievementStatusDao = AchievementStatusDao("user-achievements", dynamoDb)
        achievementStatusDao.mapper.createTable(ProvisionedThroughput(1, 1))

        steamSource = FakeSource()
        xboxSource = FakeSource()

        sourcesManager = SourcesManager(steamSource, gamesDao, achievementsDao, userGamesDao, achievementStatusDao)
        gamesManager = GamesManager(gamesDao, userGamesDao, achievementsDao, achievementStatusDao)
        syncManager = SyncManager(syncExecutor, sourcesManager)
    }

    fun createUser(displayName: String? = null, vararg players: Player): User {
        val id = UUID.randomUUID().toString()
        val user = User(
                id = id,
                displayName = displayName ?: "user-$id",
                xbox = players.firstOrNull { it.platform == Game.Platform.Xbox }?.socialLink(),
                steam = players.firstOrNull { it.platform == Game.Platform.Steam }?.socialLink()
        )
        usersDao.save(user)

        return user
    }

    fun createPlayer(platform: Game.Platform = Game.Platform.Steam, displayName: String? = null): Player {
        val id = UUID.randomUUID().toString()
        val player = Player(
                id = id,
                avatar = null,
                platform = platform,
                displayName = displayName ?: "player-$id"
        )
        platform.source().addPlayer(player)
        return player
    }

    fun createGame(platform: Game.Platform = Game.Platform.Steam, name: String? = null): Game {
        val id = UUID.randomUUID().toString()
        val game = Game(
                id = id,
                displayImage = null,
                icon = null,
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
        val source = game.platform.source()

        source.addGameToLibrary(player.id, game.id)
    }

    fun sync(user: User) {
        syncManager.sync(user)
    }

    private fun Game.Platform.source() = when(this) {
        Game.Platform.Steam -> steamSource
        Game.Platform.Xbox -> xboxSource
    }

    private fun Player.socialLink(token: String? = null) = SocialLink(
            id = id,
            username = displayName,
            platform = platform,
            token = token
    )
}