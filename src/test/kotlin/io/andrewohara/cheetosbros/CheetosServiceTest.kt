package io.andrewohara.cheetosbros

import io.andrewohara.cheetosbros.games.witImageHost
import io.andrewohara.cheetosbros.games.withImageHost
import io.andrewohara.cheetosbros.sources.AchievementStatusData
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CheetosServiceTest {

    private val driver = TestDriver()
    private val testObj = driver.service
    private val userId = "1111"

    @Test
    fun `list games for missing user`() {
        testObj.listGames("missingUser").shouldBeEmpty()
    }

    @Test
    fun `list games`() {
        val me3 = driver.createGame(userId, me3Data, me3AchievementData, emptyList())
        val satisfactory = driver.createGame(userId, satisfactoryData, satisfactoryAchievementData, emptyList())

        testObj.listGames(userId).shouldContainExactlyInAnyOrder(
            me3.game.withImageHost(testCdnHost), satisfactory.game.withImageHost(testCdnHost)
        )
    }

    @Test
    fun `list achievements for missing game`() {
        testObj.listAchievements(userId, "missingGame").shouldBeEmpty()
    }

    @Test
    fun `list achievements`() {
        val me3 = driver.createGame(userId, me3Data, me3AchievementData, emptyList())
        val satisfactory = driver.createGame(userId, satisfactoryData, satisfactoryAchievementData, emptyList())

        testObj.listAchievements(userId, me3.game.id) shouldContainExactlyInAnyOrder  me3.achievements.witImageHost(testCdnHost)
        testObj.listAchievements(userId, satisfactory.game.id) shouldContainExactlyInAnyOrder satisfactory.achievements.witImageHost(testCdnHost)
    }

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

        driver.gamesDao[userId].withImageHost(testCdnHost) shouldContainExactlyInAnyOrder results
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

        driver.gamesDao[userId].withImageHost(testCdnHost) shouldContainExactlyInAnyOrder results
        driver.achievementsDao[userId, godOfWarData.id] shouldHaveSize 2
        driver.achievementsDao[userId, factorioData.id] shouldHaveSize 1
    }

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

    @Test
    fun `set game favourite - not found`() {
        testObj.updateGame(userId, godOfWarData.id, favourite = true) shouldBe null
    }

    @Test
    fun `set game favourite`() {
        val created = driver.createGame(userId, godOfWarData, emptyList(), emptyList())

        val expected = created.game.copy(favourite = true)
        testObj.updateGame(userId, godOfWarData.id, favourite = true) shouldBe expected
        driver.gamesDao[userId, godOfWarData.id] shouldBe expected
    }

    @Test
    fun `set game not favourite`() {
        val created = driver.createGame(userId, godOfWarData, emptyList(), emptyList(), favourite = true)

        val expected = created.game.copy(favourite = false)
        testObj.updateGame(userId, godOfWarData.id, favourite = false) shouldBe expected
        driver.gamesDao[userId, godOfWarData.id] shouldBe expected
    }

    @Test
    fun `set achievement favourite - not found`() {
        driver.createGame(userId, godOfWarData, emptyList(), emptyList())

        testObj.updateAchievement(userId, godOfWarData.id, godOfWarAchievement1Data.id, favourite = true).shouldBeNull()
    }

    @Test
    fun `set achievement favourite`() {
        driver.createGame(userId, godOfWarData, listOf(godOfWarAchievement1Data), emptyList())

        testObj.updateAchievement(userId, godOfWarData.id, godOfWarAchievement1Data.id, favourite = true)
            .shouldNotBeNull()
            .favourite shouldBe true
    }

    @Test
    fun `get friends`() {
        val friend1 = driver.steam.createUser("tom")
        val friend2 = driver.steam.createUser("hank")

        driver.steam.addFriend(userId = userId.toLong(), friendId = friend1.id.toLong())
        driver.steam.addFriend(userId = userId.toLong(), friendId = friend2.id.toLong())

        testObj.getFriends(userId).shouldContainExactlyInAnyOrder(
            friend1, friend2
        )
    }

    @Test
    fun `list achievements status`() {
        driver.steam += godOfWarData
        driver.steam += godOfWarAchievement1Data
        driver.steam += godOfWarAchievement2Data
        driver.steam.unlockAchievement(userId, godOfWarAchievement1Data, tNow)

        testObj.listAchievementStatus(gameId = godOfWarData.id, userId = userId) shouldContainExactlyInAnyOrder listOf(
            AchievementStatusData(achievementId = godOfWarAchievement1Data.id, unlockedOn = tNow),
            AchievementStatusData(achievementId = godOfWarAchievement2Data.id, unlockedOn = null)
        )
    }
}