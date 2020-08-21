package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.TestDriver
import io.andrewohara.cheetosbros.api.games.v1.GamesManager
import org.assertj.core.api.Assertions.*
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class SourcesManagerTest {

    @Rule @JvmField val driver = TestDriver()

    @Test
    fun `discover games for single platform`() {
        val player = driver.createPlayer()

        val game1 = driver.createGame()
        driver.addToLibrary(game1, player)

        val game2 = driver.createGame()
        driver.addToLibrary(game2, player)

        driver.createGame()

        val user = driver.createUser("user1", player)

        val result = driver.sourcesManager.discoverGames(user, player.platform)

        assertThat(result).containsExactlyInAnyOrder(game1, game2)
        assertThat(driver.gamesManager.listGames(user)).isEmpty()
    }

    @Test
    fun `sync game with achievements`() {
        val player = driver.createPlayer()

        val game = driver.createGame()
        driver.addToLibrary(game, player)
        val cheeto1 = driver.createAchievement(game, "Complete the Tutorial")
        val cheeto2 = driver.createAchievement(game, "Complete the Game")
        val status1 = driver.unlockAchievement(player, game, cheeto1, Instant.ofEpochSecond(1000))

        val user = driver.createUser("user1", player)

        driver.sourcesManager.syncGame(user, game)

        assertThat(driver.gamesManager.listGames(user)).containsExactly(game)
        assertThat(driver.gamesManager.listAchievements(user, game.uuid)).containsExactlyInAnyOrder(
                GamesManager.AchievementDetails(achievement = cheeto1, status = status1),
                GamesManager.AchievementDetails(achievement = cheeto2, status = AchievementStatus(achievementId = cheeto2.id, unlockedOn = null))
        )
    }
}