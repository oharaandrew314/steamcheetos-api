package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.TestDriver
import org.assertj.core.api.Assertions.*
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class SyncManagerTest {

    @Rule @JvmField val driver = TestDriver()

    private val sourceHelper = driver.sourceHelper

    @Test
    fun `sync games across single platform`() {
        val player = sourceHelper.createPlayer()

        val game1 = sourceHelper.createGame()
        sourceHelper.addToLibrary(game1, player)

        val game2 = sourceHelper.createGame()
        sourceHelper.addToLibrary(game2, player)

        sourceHelper.createGame()

        val user = sourceHelper.createUser("user1", player)
        driver.syncManager.sync(user)

        assertThat(driver.gamesManager.listGames(user)).containsExactlyInAnyOrder(game1, game2)
    }

    @Test
    fun `sync game with achievements`() {
        val player = sourceHelper.createPlayer()

        val game = sourceHelper.createGame()
        sourceHelper.addToLibrary(game, player)
        val cheeto1 = sourceHelper.createAchievement(game, "Complete the Tutorial")
        val cheeto2 = sourceHelper.createAchievement(game, "Complete the Game")
        val status1 = sourceHelper.unlockAchievement(player, game, cheeto1, Instant.ofEpochSecond(1000))

        val user = sourceHelper.createUser("user1", player)
        sourceHelper.sync(user)

        assertThat(driver.gamesManager.listGames(user)).containsExactly(game)
        assertThat(driver.gamesManager.listAchievementStatus(user, playerId = player.id, gameId = game.id)).containsExactlyInAnyOrder(
                status1,
                AchievementStatus(achievementId = cheeto2.id, unlockedOn = null)
        )
    }

    @Test
    fun `discover friends across multiple platforms`() {
        val steamPlayer = sourceHelper.createPlayer(Platform.Steam, displayName = "My steam account")
        val xboxPlayer = sourceHelper.createPlayer(Platform.Xbox, displayName = "My xbox account")

        val steamFriend = sourceHelper.createPlayer(platform = Platform.Steam, displayName = "steam friend")
        sourceHelper.addFriend(player = steamPlayer, friend = steamFriend)

        val xboxFriend = sourceHelper.createPlayer(platform = Platform.Xbox, displayName = "xbox friend")
        sourceHelper.addFriend(player = xboxPlayer, friend = xboxFriend)

        val user = sourceHelper.createUser("user1", steamPlayer, xboxPlayer)
        sourceHelper.sync(user)

        assertThat(driver.usersManager.getFriends(user)).containsExactly(steamFriend, xboxFriend)
    }

    @Test
    fun `discover friends twice when there are new ones in between`() {
        val player = sourceHelper.createPlayer()
        val user = sourceHelper.createUser("user1", player)

        val friend1 = sourceHelper.createPlayer()
        sourceHelper.addFriend(player = player, friend = friend1)

        sourceHelper.sync(user)
        assertThat(driver.usersManager.getFriends(user)).hasSize(1)

        val friend2 = sourceHelper.createPlayer()
        sourceHelper.addFriend(player = player, friend = friend2)

        sourceHelper.sync(user)
        assertThat(driver.usersManager.getFriends(user)).hasSize(2)
    }

    @Test
    fun `discover friends twice when some were deleted in between`() {
        val player = sourceHelper.createPlayer()
        val user = sourceHelper.createUser("user1", player)

        val friend1 = sourceHelper.createPlayer()
        sourceHelper.addFriend(player = player, friend = friend1)

        val friend2 = sourceHelper.createPlayer()
        sourceHelper.addFriend(player = player, friend = friend2)

        sourceHelper.sync(user)
        assertThat(driver.usersManager.getFriends(user)).hasSize(2)

        sourceHelper.removeFriend(player = player, friend = friend1)

        sourceHelper.sync(user)
        assertThat(driver.usersManager.getFriends(user)).hasSize(1)
    }
}