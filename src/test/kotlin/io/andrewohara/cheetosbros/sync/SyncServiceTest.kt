package io.andrewohara.cheetosbros.sync

import io.andrewohara.cheetosbros.TestDriver
import io.andrewohara.cheetosbros.tnow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SyncServiceTest {

    private val driver = TestDriver()

    private val testObj = driver.syncService
    private val userId = "1337"

    @Test
    fun `discover games`() {

    }

    @Test
    fun `sync multiple games`() {
        val game1 = driver.steam.createGame("God of War: Atreus")
        val game1Cheeto1 = driver.steam.createAchievement(game1, "Boy")
        driver.steam.unlockAchievement(userId, game1Cheeto1, tnow)
        driver.steam.createAchievement(game1, "Don't be Sorry!", "Be Better")
        val result1 = testObj.syncGame(userId, game1.id).shouldNotBeNull()

        val game2 = driver.steam.createGame("Factorio")
        driver.steam.createAchievement(game2, "Trains have right of way", "Get killed by your own locomotive")
        val result2 = testObj.syncGame(userId, game2.id).shouldNotBeNull()

        driver.gamesDao[userId].shouldContainExactlyInAnyOrder(result1.game, result2.game)
        driver.achievementsDao[userId, result1.game.id].shouldContainExactly(result1.achievements)
        driver.achievementsDao[userId, result2.game.id].shouldContainExactly(result2.achievements)
    }

    @Test
    fun `sync a game with no achievements - not worth saving`() {
        val game = driver.steam.createGame("Core Keeper")
        testObj.syncGame(userId, game.id).shouldBeNull()

        driver.gamesDao[userId].shouldBeEmpty()
    }

    @Test
    fun `sync game where user has partial progress with achievements`() {
        val game = driver.steam.createGame("God of War: Atreus")
        val cheeto1 = driver.steam.createAchievement(game, "Boy")
        driver.steam.unlockAchievement(userId, cheeto1, tnow)
        val cheeto2 = driver.steam.createAchievement(game, "Don't be Sorry!", "Be Better")

        val result = testObj.syncGame(userId, game.id).shouldNotBeNull()
        result.game.name shouldBe "God of War: Atreus"
        result.game.achievementsTotal shouldBe 2
        result.game.achievementsUnlocked shouldBe 1
        result.achievements shouldHaveSize 2
        result.achievements
            .find { it.id == cheeto1.id }
            .shouldNotBeNull()
            .unlockedOn shouldBe tnow
        result.achievements
            .find { it.id == cheeto2.id }
            .shouldNotBeNull()
            .unlockedOn.shouldBeNull()

        driver.gamesDao[userId, game.id] shouldBe result.game
        driver.achievementsDao[userId, game.id] shouldContainExactly result.achievements
    }
}