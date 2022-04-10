package io.andrewohara.cheetosbros

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CheetosServiceTest {

    private val driver = TestDriver()
    private val testObj = driver.service
    private val userId = "1111"

    // list games

    @Test
    fun `list games for missing user`() {
        testObj.listGames("missingUser").shouldBeEmpty()
    }

    @Test
    fun `list games`() {
        val me3 = driver.createGame(userId, me3Data, me3AchievementData, emptyList())
        val satisfactory = driver.createGame(userId, satisfactoryData, satisfactoryAchievementData, emptyList())

        testObj.listGames(userId).shouldContainExactlyInAnyOrder(
            me3.game, satisfactory.game
        )
    }

    // list achievements

    @Test
    fun `list achievements for missing game`() {
        testObj.listAchievements(userId, "missingGame").shouldBeEmpty()
    }

    @Test
    fun `list achievements`() {
        val me3 = driver.createGame(userId, me3Data, me3AchievementData, emptyList())
        val satisfactory = driver.createGame(userId, satisfactoryData, satisfactoryAchievementData, emptyList())

        testObj.listAchievements(userId, me3.game.id) shouldContainExactlyInAnyOrder  me3.achievements
        testObj.listAchievements(userId, satisfactory.game.id) shouldContainExactlyInAnyOrder satisfactory.achievements
    }

    // refresh games

    @Test
    fun `refresh games - none yet saved, and none played recently`() {
        driver.steam += godOfWarData
        driver.steam += godOfWarAchievement1Data
        driver.steam += godOfWarAchievement2Data
        driver.steam[userId] = godOfWarData
        driver.steam.unlockAchievement(userId, godOfWarAchievement1Data, tNow)

        driver.steam += factorioData
        driver.steam += factorioTrainCheeto
        driver.steam[userId] = factorioData

        val results = testObj.refreshGames(userId).shouldHaveSize(2)

        driver.gamesDao[userId] shouldContainExactlyInAnyOrder results
        driver.achievementsDao[userId, godOfWarData.id] shouldHaveSize 0
        driver.achievementsDao[userId, factorioData.id] shouldHaveSize 0
    }

    @Test
    fun `refresh games - none yet saved, but played recently`() {
        driver.steam += godOfWarData
        driver.steam += godOfWarAchievement1Data
        driver.steam += godOfWarAchievement2Data
        driver.steam[userId] = godOfWarData
        driver.steam.recentlyPlayed(userId, godOfWarData)

        driver.steam += factorioData
        driver.steam += factorioTrainCheeto
        driver.steam[userId] = factorioData
        driver.steam.recentlyPlayed(userId, factorioData)

        val results = testObj.refreshGames(userId).shouldHaveSize(2)

        driver.gamesDao[userId] shouldContainExactlyInAnyOrder results
        driver.achievementsDao[userId, godOfWarData.id] shouldHaveSize 2
        driver.achievementsDao[userId, factorioData.id] shouldHaveSize 1
    }

    // refresh achievements

    @Test
    fun `sync achievements for game where user has progress`() {
        driver.steam += godOfWarData
        driver.steam += godOfWarAchievement1Data
        driver.steam += godOfWarAchievement2Data
        driver.steam[userId] = godOfWarData
        driver.createGame(userId, godOfWarData, emptyList(), emptyList())

        driver.steam.unlockAchievement(userId, godOfWarAchievement1Data, tNow)

        testObj.refreshAchievements(userId, godOfWarData.id)
            .shouldNotBeNull()
            .shouldHaveSize(2)
            .count { it.unlockedOn != null } shouldBe 1
    }
}