package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.api.ApiTestDriver
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class SourceManagerTest {

    @Rule @JvmField val apiDriver = ApiTestDriver()
    @Rule @JvmField val sourceDriver = SourceTestDriver()

    private lateinit var testObj: SourceManager

    @Before
    fun setup() {
        testObj = SourceManager(
            sourceFactory = sourceDriver.sourceFactory,
            gamesDao = apiDriver.gamesDao,
            gameLibraryDao = apiDriver.gameLibraryDao,
            achievementsDao = apiDriver.achievementsDao,
            achievementStatusDao = apiDriver.achievementStatusDao
        )
    }

    @Test
    fun `sync library`() {
        val player = sourceDriver.createPlayer(Platform.Steam)

        val game1 = sourceDriver.createGame(player.platform)
        sourceDriver.addToLibrary(player, game1)

        val game2 = sourceDriver.createGame(Platform.Steam)
        sourceDriver.addToLibrary(player, game2)

        sourceDriver.createGame(Platform.Steam)

        testObj.sync(player, sourceDriver.source(player))

        assertThat(apiDriver.gameLibraryDao.listGameIds(player)).containsExactlyInAnyOrder(game1.id, game2.id)
        assertThat(apiDriver.gamesDao.batchGet(player.platform, listOf(game1.id, game2.id))).containsExactlyInAnyOrder(game1, game2)
    }

    @Test
    fun `sync game with achievements`() {
        val player = sourceDriver.createPlayer(Platform.Steam)

        val game = sourceDriver.createGame(Platform.Steam)
        sourceDriver.addToLibrary(player, game)
        val achievement1 = sourceDriver.createAchievement(game, "Complete the Tutorial")
        val achievement2 = sourceDriver.createAchievement(game, "Complete the Game")
        val status1 = sourceDriver.unlockAchievement(player, game, achievement1, Instant.ofEpochSecond(1000))

        testObj.sync(player, sourceDriver.source(player))

        assertThat(apiDriver.achievementsDao[game]).containsExactlyInAnyOrder(achievement1, achievement2)
        assertThat(apiDriver.achievementStatusDao[player, game]).containsExactlyInAnyOrder(
            status1,
            AchievementStatus(achievementId = achievement2.id, unlockedOn = null)
        )
    }
}