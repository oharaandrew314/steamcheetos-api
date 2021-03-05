package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.api.ApiTestDriver
import io.andrewohara.cheetosbros.api.games.CachedGame
import io.andrewohara.cheetosbros.api.games.OwnedGame
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.Instant

class SourceManagerTest {

    @Rule @JvmField val apiDriver = ApiTestDriver
    @Rule @JvmField val sourceDriver = SourceTestDriver()

    private lateinit var testObj: SourceManager

    @Before
    fun setup() {
        testObj = SourceManager(
            sourceFactory = sourceDriver.sourceFactory,
            gamesDao = apiDriver.gamesDao,
            gameLibraryDao = apiDriver.libraryDao,
            achievementsDao = apiDriver.achievementsDao,
            achievementStatusDao = apiDriver.achievementStatusDao,
            gameCacheDuration = Duration.ofSeconds(30),
            timeSupplier = { apiDriver.time }
        )
    }

    @Test
    fun `sync multiple games`() {
        val player = sourceDriver.createPlayer(Platform.Steam)

        val game1 = sourceDriver.createGame(player.uid.platform)
        val game1Achievement = sourceDriver.createAchievement(game1)
        sourceDriver.addToLibrary(player, game1)
        sourceDriver.unlockAchievement(player, game1, game1Achievement, apiDriver.time)

        val game2 = sourceDriver.createGame(Platform.Steam)
        val game2Achievement = sourceDriver.createAchievement(game2)
        sourceDriver.addToLibrary(player, game2)

        sourceDriver.createGame(Platform.Steam)

        testObj.syncGame(player, game1)
        testObj.syncGame(player, game2)

        assertThat(apiDriver.libraryDao[player]).containsExactlyInAnyOrder(
            OwnedGame(game1.uid, 1, apiDriver.time),
            OwnedGame(game2.uid, 0, apiDriver.time)
        )
        assertThat(apiDriver.gamesDao.batchGet(listOf(game1.uid, game2.uid))).containsExactlyInAnyOrder(
            CachedGame.create(game1, 1, apiDriver.time),
            CachedGame.create(game2, 1, apiDriver.time)
        )
        assertThat(apiDriver.achievementsDao[game1.uid]).containsExactly(game1Achievement)
        assertThat(apiDriver.achievementsDao[game2.uid]).containsExactly(game2Achievement)
    }

    @Test
    fun `sync game where user has partial progress with achievements`() {
        val player = sourceDriver.createPlayer(Platform.Steam)

        val game = sourceDriver.createGame(Platform.Steam)
        sourceDriver.addToLibrary(player, game)
        val achievement1 = sourceDriver.createAchievement(game, "Complete the Tutorial")
        val achievement2 = sourceDriver.createAchievement(game, "Complete the Game")
        val status1 = sourceDriver.unlockAchievement(player, game, achievement1, Instant.ofEpochSecond(1000))

        testObj.syncGame(player, game)

        assertThat(apiDriver.gamesDao[game.uid]).isEqualTo(CachedGame.create(game, 2, apiDriver.time))
        assertThat(apiDriver.achievementsDao[game.uid]).containsExactlyInAnyOrder(achievement1, achievement2)
        assertThat(apiDriver.achievementStatusDao[player.uid, game.uid]).containsExactlyInAnyOrder(
            status1,
            AchievementStatus(achievementId = achievement2.id, unlockedOn = null)
        )
    }

    @Test
    fun `sync game with no achievements`() {
        val player = sourceDriver.createPlayer(Platform.Steam)

        val game = sourceDriver.createGame(Platform.Steam)
        sourceDriver.addToLibrary(player, game)

        testObj.syncGame(player, game)

        assertThat(apiDriver.gamesDao[game.uid]).isEqualTo(CachedGame.create(game, 0, apiDriver.time))
        assertThat(apiDriver.libraryDao[player]).containsExactly(
            OwnedGame(game.uid, 0, apiDriver.time)
        )
        assertThat(apiDriver.achievementsDao[game.uid]).isEmpty()
    }
}