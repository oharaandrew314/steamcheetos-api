package io.andrewohara.cheetosbros.sources

import org.junit.rules.ExternalResource
import java.time.Instant
import java.util.*

class SourceTestDriver: ExternalResource() {

    private val steamSource = FakeSource(Platform.Steam)
    private val xboxSource = FakeSource(Platform.Xbox)

    val sourceFactory = FakeSourceFactory(
        steamSource = steamSource,
        xboxSource = xboxSource
    )

    override fun after() {
        steamSource.clear()
        xboxSource.clear()
    }

    private fun Platform.source() = when(this) {
        Platform.Steam -> steamSource
        Platform.Xbox -> xboxSource
    }

    fun source(player: Player) = player.platform.source()

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
        player.platform.source().addGameToLibrary(player.id, game.id)
    }
}