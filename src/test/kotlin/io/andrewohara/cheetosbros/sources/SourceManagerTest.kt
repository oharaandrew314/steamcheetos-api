package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.TestDriver
import io.andrewohara.cheetosbros.api.games.CachedGame
import io.andrewohara.cheetosbros.api.games.OwnedGame
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class SourceManagerTest {

    @Rule @JvmField val driver = TestDriver

    private lateinit var testObj: SourceManager

    @Before
    fun setup() {
        testObj = driver.sourceManager
    }

    @Test
    fun `sync multiple games`() {
        val player = driver.createPlayer(Platform.Steam)

        val gameData1 = driver.createGame(player.uid.platform)
        val game1Achievement = driver.createAchievement(gameData1)
        driver.addToLibrary(player, gameData1)
        driver.unlockAchievement(player, gameData1, game1Achievement, driver.time)
        val game1 = driver.saveGame(gameData1)

        val gameData2 = driver.createGame(Platform.Steam)
        val game2Achievement = driver.createAchievement(gameData2)
        driver.addToLibrary(player, gameData2)
        val game2 = driver.saveGame(gameData2)

        driver.createGame(Platform.Steam)

        testObj.syncGame(player, game1, driver.time)
        testObj.syncGame(player, game2, driver.time)

        assertThat(driver.libraryDao[player]).containsExactlyInAnyOrder(
            OwnedGame(gameData1.uid, 1, driver.time),
            OwnedGame(gameData2.uid, 0, driver.time)
        )
        assertThat(driver.gamesDao.batchGet(listOf(gameData1.uid, gameData2.uid))).containsExactlyInAnyOrder(
            CachedGame.create(gameData1, 1, driver.time),
            CachedGame.create(gameData2, 1, driver.time)
        )
        assertThat(driver.achievementsDao[gameData1.uid]).containsExactly(game1Achievement)
        assertThat(driver.achievementsDao[gameData2.uid]).containsExactly(game2Achievement)
    }

    @Test
    fun `sync game where user has partial progress with achievements`() {
        val player = driver.createPlayer(Platform.Steam)

        val gameData = driver.createGame(Platform.Steam)
        driver.addToLibrary(player, gameData)
        val game = driver.saveGame(gameData)
        val achievement1 = driver.createAchievement(gameData, "Complete the Tutorial")
        val achievement2 = driver.createAchievement(gameData, "Complete the Game")
        val status1 = driver.unlockAchievement(player, gameData, achievement1, Instant.ofEpochSecond(1000))

        testObj.syncGame(player, game, driver.time)

        assertThat(driver.gamesDao[gameData.uid]).isEqualTo(CachedGame.create(gameData, 2, driver.time))
        assertThat(driver.achievementsDao[gameData.uid]).containsExactlyInAnyOrder(achievement1, achievement2)
        assertThat(driver.achievementStatusDao[player.uid, gameData.uid]).containsExactlyInAnyOrder(
            status1,
            AchievementStatus(achievementId = achievement2.id, unlockedOn = null)
        )
    }

    @Test
    fun `sync game with no achievements`() {
        val player = driver.createPlayer(Platform.Steam)

        val gameData = driver.createGame(Platform.Steam)
        driver.addToLibrary(player, gameData)
        val game = driver.saveGame(gameData)

        testObj.syncGame(player, game, driver.time)

        assertThat(driver.gamesDao[gameData.uid]).isEqualTo(CachedGame.create(gameData, 0, driver.time))
        assertThat(driver.libraryDao[player]).containsExactly(
            OwnedGame(gameData.uid, 0, driver.time)
        )
        assertThat(driver.achievementsDao[gameData.uid]).isEmpty()
    }
}