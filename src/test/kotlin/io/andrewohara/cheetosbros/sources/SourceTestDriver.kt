package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.api.games.Uid
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

    fun createGame(platform: Platform, name: String? = null): Source.Game {
        val id = UUID.randomUUID().toString()
        val game = Source.Game(
            uid = Uid(platform, id),
            displayImage = null,
            name = name ?: "game-$id",
        )
        platform.source().addGame(game)

        return game
    }

    fun createAchievement(game: Source.Game, name: String? = null, description: String? = null, hidden: Boolean = false, score: Int? = null): Achievement {
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
        game.uid.platform.source().addAchievement(game.uid.id, achievement)

        return achievement
    }

    fun unlockAchievement(player: Player, game: Source.Game, achievement: Achievement, unlocked: Instant?): AchievementStatus {
        val status = AchievementStatus(
            achievementId = achievement.id,
            unlockedOn = unlocked
        )

        game.uid.platform.source().addUserAchievement(game.uid.id, player.uid.id, status)

        return status
    }

    fun addToLibrary(player: Player, game: Source.Game) {
        player.uid.platform.source().addGameToLibrary(player.uid.id, game.uid.id)
    }
}