package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.TestDriver
import io.andrewohara.cheetosbros.api.games.v1.GamesManager
import io.andrewohara.cheetosbros.api.users.FriendsDao
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

    @Test
    fun `discover friends`() {
        val player = driver.createPlayer()
        val user = driver.createUser("user1", player)

        val friend1 = driver.createPlayer()
        driver.addFriend(player = player, friend = friend1)

        val friend2 = driver.createPlayer()
        driver.addFriend(player = player, friend = friend2)

        assertThat(driver.sourcesManager.discoverFriends(user, player.platform)).containsExactlyInAnyOrder(
                FriendsDao.Friend(id = friend1.id, platform = friend1.platform),
                FriendsDao.Friend(id = friend2.id, platform = friend2.platform)
        )
        assertThat(driver.usersManager.getFriends(user)).containsExactlyInAnyOrder(
                Player(id = friend1.id, platform = friend1.platform, username = "Unknown Friend: ${friend1.id}", avatar = null),
                Player(id = friend2.id, platform = friend2.platform, username = "Unknown Friend: ${friend2.id}", avatar = null)
        )
    }

    @Test
    fun `discover friends for two platforms`() {
        val steamPlayer = driver.createPlayer(Game.Platform.Steam)
        val xboxPLayer = driver.createPlayer(Game.Platform.Xbox)
        val user = driver.createUser("user1", steamPlayer, xboxPLayer)

        val steamFriend = driver.createPlayer(Game.Platform.Steam)
        driver.addFriend(player = steamPlayer, friend = steamFriend)

        val xboxFriend = driver.createPlayer(Game.Platform.Xbox)
        driver.addFriend(player = xboxPLayer, friend = xboxFriend)

        assertThat(driver.sourcesManager.discoverFriends(user, Game.Platform.Steam)).containsExactlyInAnyOrder(
                FriendsDao.Friend(id = steamFriend.id, platform = steamFriend.platform)
        )
        assertThat(driver.sourcesManager.discoverFriends(user, Game.Platform.Xbox)).containsExactlyInAnyOrder(
                FriendsDao.Friend(id = xboxFriend.id, platform = xboxFriend.platform)
        )

        assertThat(driver.usersManager.getFriends(user)).containsExactlyInAnyOrder(
                Player(id = steamFriend.id, platform = steamFriend.platform, username = "Unknown Friend: ${steamFriend.id}", avatar = null),
                Player(id = xboxFriend.id, platform = xboxFriend.platform, username = "Unknown Friend: ${xboxFriend.id}", avatar = null)
        )
    }

    @Test
    fun `discover friends twice when there are new ones in between`() {
        val player = driver.createPlayer()
        val user = driver.createUser("user1", player)

        val friend1 = driver.createPlayer()
        driver.addFriend(player = player, friend = friend1)

        assertThat(driver.sourcesManager.discoverFriends(user, player.platform)).hasSize(1)
        assertThat(driver.usersManager.getFriends(user)).hasSize(1)

        val friend2 = driver.createPlayer()
        driver.addFriend(player = player, friend = friend2)

        assertThat(driver.sourcesManager.discoverFriends(user, player.platform)).hasSize(2)
        assertThat(driver.usersManager.getFriends(user)).hasSize(2)
    }

    @Test
    fun `discover friends twice when some were deleted in between`() {
        val player = driver.createPlayer()
        val user = driver.createUser("user1", player)

        val friend1 = driver.createPlayer()
        driver.addFriend(player = player, friend = friend1)

        val friend2 = driver.createPlayer()
        driver.addFriend(player = player, friend = friend2)

        assertThat(driver.sourcesManager.discoverFriends(user, player.platform)).hasSize(2)
        assertThat(driver.usersManager.getFriends(user)).hasSize(2)

        driver.removeFriend(player = player, friend = friend1)

        assertThat(driver.sourcesManager.discoverFriends(user, player.platform)).hasSize(1)
        assertThat(driver.usersManager.getFriends(user)).hasSize(1)
    }

    @Test
    fun `discover and sync friend`() {
        val player = driver.createPlayer()
        val user = driver.createUser("user1", player)

        val player2 = driver.createPlayer()
        driver.addFriend(player, player2)

        val friends = driver.sourcesManager.discoverFriends(user, player.platform)
        for (friend in friends) {
            driver.sourcesManager.syncFriend(user, friend)
        }

        assertThat(driver.usersManager.getFriends(user)).containsExactly(player2)
    }
}