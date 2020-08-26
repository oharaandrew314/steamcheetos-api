package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.TestDriver
import org.assertj.core.api.Assertions.*
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class SourceManagerTest {

    @Rule @JvmField val driver = TestDriver()

    @Test
    fun `sync library across single platform`() {
        val player = driver.createPlayer(Platform.Steam)

        val game1 = driver.createGame(Platform.Steam)
        driver.addToLibrary(player, game1)

        val game2 = driver.createGame(Platform.Steam)
        driver.addToLibrary(player, game2)

        driver.createGame(Platform.Steam)

        val user = driver.createUser("user1", player)
        driver.sourceManager.getLibrary(user, player)

        assertThat(driver.gamesManager.listGames(user)).containsExactlyInAnyOrder(game1, game2)
    }

    @Test
    fun `sync library`() {
        val player = driver.createPlayer(Platform.Steam)

        val game1 = driver.createGame(Platform.Steam)
        driver.addToLibrary(player, game1)

        val game2 = driver.createGame(Platform.Steam)
        driver.addToLibrary(player, game2)

        val user = driver.createUser()

        assertThat(driver.sourceManager.getLibrary(user, player)).containsExactlyInAnyOrder(game1, game2)
    }

    @Test
    fun `sync game achievements`() {
        val game = driver.createGame(Platform.Xbox)
        val cheeto1 = driver.createAchievement(game, "Complete the Tutorial")
        val cheeto2 = driver.createAchievement(game, "Complete the Game")

        val player = driver.createPlayer(Platform.Xbox)
        driver.addToLibrary(player, game)

        val user = driver.createUser(xbox = player)

        assertThat(driver.sourceManager.getAchievements(user, game)).containsExactlyInAnyOrder(cheeto1, cheeto2)
    }

    @Test
    fun `sync achievements statuses`() {
        val player = driver.createPlayer(Platform.Xbox)

        val game = driver.createGame(Platform.Xbox)
        driver.addToLibrary(player, game)
        val cheeto1 = driver.createAchievement(game, "Complete the Tutorial")
        driver.createAchievement(game, "Complete the Game")
        val status1 = driver.unlockAchievement(player, game, cheeto1, Instant.ofEpochSecond(1000))

        val user = driver.createUser(xbox = player)

        assertThat(driver.sourceManager.getAchievementStatus(user, game, player)).containsExactlyInAnyOrder(status1)
    }

    @Test
    fun `discover friends`() {
        val player = driver.createPlayer(Platform.Steam, displayName = "My steam account")

        val friend1 = driver.createPlayer(platform = Platform.Steam, displayName = "steam friend")
        driver.addFriend(player = player, friend = friend1)

        val friend2 = driver.createPlayer(platform = Platform.Steam, displayName = "xbox friend")
        driver.addFriend(player = player, friend = friend2)

        val user = driver.createUser(steam = player)

        assertThat(driver.sourceManager.getFriends(user, player)).containsExactlyInAnyOrder(friend1.id, friend2.id)
    }

    @Test
    fun `discover friends twice when there are new ones in between`() {
        val player = driver.createPlayer(Platform.Steam)
        val user = driver.createUser(steam = player)

        val friend1 = driver.createPlayer(Platform.Steam)
        driver.addFriend(player = player, friend = friend1)

        assertThat(driver.sourceManager.getFriends(user, player)).containsExactlyInAnyOrder(friend1.id)

        val friend2 = driver.createPlayer(Platform.Steam)
        driver.addFriend(player = player, friend = friend2)

        assertThat(driver.sourceManager.getFriends(user, player)).containsExactlyInAnyOrder(friend1.id, friend2.id)
    }

    @Test
    fun `discover friends twice when some were deleted in between`() {
        val player = driver.createPlayer(Platform.Xbox)
        val user = driver.createUser(xbox = player)

        val friend1 = driver.createPlayer(Platform.Xbox)
        driver.addFriend(player = player, friend = friend1)

        val friend2 = driver.createPlayer(Platform.Xbox)
        driver.addFriend(player = player, friend = friend2)

        assertThat(driver.sourceManager.getFriends(user, player)).containsExactlyInAnyOrder(friend1.id, friend2.id)

        driver.removeFriend(player = player, friend = friend1)

        assertThat(driver.sourceManager.getFriends(user, player)).containsExactlyInAnyOrder(friend2.id)
    }
}