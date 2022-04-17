package io.andrewohara.cheetosbros.api.v1

import io.andrewohara.cheetosbros.*
import io.andrewohara.cheetosbros.games.withHost
import io.kotest.matchers.be
import io.kotest.matchers.collections.containExactly
import io.kotest.matchers.collections.containExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.http4k.core.*
import org.http4k.kotest.shouldHaveBody
import org.http4k.kotest.shouldHaveStatus
import org.junit.jupiter.api.Test

class ApiV1Test {

    private val driver = TestDriver()
    private val userId = "1337"

    private fun Request.asUser(id: String) = header("Authorization", "Bearer $id")

    @Test
    fun `list games - unauthorized`() {
        val request = Request(Method.GET, "/v1/games")

        driver(request) shouldHaveStatus Status.UNAUTHORIZED
    }

    @Test
    fun `list games`() {
        val created = driver.createGame(userId, me3Data, me3AchievementData, emptyList())

        val response = Request(Method.GET, "/v1/games")
            .asUser(userId)
            .let(driver)

        response shouldHaveStatus Status.OK
        response.shouldHaveBody(ApiV1.Lenses.gamesList, containExactly(created.game.toDtoV1().withImageHost(testCdnHost)))
    }

    @Test
    fun `list achievements`() {
        val created = driver.createGame(userId, me3Data, me3AchievementData, emptyList())

        val response = Request(Method.GET, "/v1/games/${created.game.id}/achievements")
            .asUser(userId)
            .let(driver)

        response shouldHaveStatus Status.OK
        response.shouldHaveBody(ApiV1.Lenses.achievementList, containExactlyInAnyOrder(created.achievements.toDtoV1s().withImageHost(testCdnHost)))
    }

    @Test
    fun `get user - not available`() {
        val response = Request(Method.GET, "/v1/user")
            .asUser(userId)
            .let(driver)

        response shouldHaveStatus Status.NO_CONTENT
    }

    @Test
    fun `get user`() {
        val user = driver.steam.createUser("xxCheetoHunter420xx", "https://slayer.jpg")

        val response = Request(Method.GET, "/v1/user")
            .asUser(user.id)
            .let(driver)

        response shouldHaveStatus Status.OK
        response.shouldHaveBody(ApiV1.Lenses.user, be(
            UserDtoV1(
                name = "xxCheetoHunter420xx",
                avatar = Uri.of("https://slayer.jpg")
            )
        ))
    }

    @Test
    fun `set game favourite - not found`() {
        Request(Method.PUT, "/v1/games/${godOfWarData.id}")
            .with(ApiV1.Lenses.updateGame of UpdateGameRequestV1(favourite = true))
            .asUser(userId)
            .let(driver)
            .shouldHaveStatus(Status.NOT_FOUND)
    }

    @Test
    fun `set game favourite`() {
        driver.createGame(userId, godOfWarData, emptyList(), emptyList())

        val response = Request(Method.PUT, "/v1/games/${godOfWarData.id}")
            .with(ApiV1.Lenses.updateGame of UpdateGameRequestV1(favourite = true))
            .asUser(userId)
            .let(driver)

        response shouldHaveStatus Status.OK
        ApiV1.Lenses.game(response).favourite shouldBe true

        driver.gamesDao[userId, godOfWarData.id].shouldNotBeNull()
            .favourite shouldBe true
    }

    @Test
    fun `set achievement favourite - not found`() {
        driver.createGame(userId, godOfWarData, emptyList(), emptyList())

        Request(Method.PUT, "/v1/games/${godOfWarData.id}/achievements/${godOfWarAchievement1Data.id}")
            .with(ApiV1.Lenses.updateGame of UpdateGameRequestV1(favourite = true))
            .asUser(userId)
            .let(driver)
            .shouldHaveStatus(Status.NOT_FOUND)
    }

    @Test
    fun `set achievement favourite`() {
        driver.createGame(userId, godOfWarData, listOf(godOfWarAchievement1Data), emptyList())

        val response = Request(Method.PUT, "/v1/games/${godOfWarData.id}/achievements/${godOfWarAchievement1Data.id}")
            .with(ApiV1.Lenses.updateGame of UpdateGameRequestV1(favourite = true))
            .asUser(userId)
            .let(driver)

        response shouldHaveStatus Status.OK
        ApiV1.Lenses.achievement(response).favourite shouldBe true

        driver.achievementsDao[userId, godOfWarData.id, godOfWarAchievement1Data.id].shouldNotBeNull()
            .favourite shouldBe true
    }

    private fun Collection<AchievementDtoV1>.withImageHost(host: Uri) = map {
        it.copy(
            iconLocked = Uri.of(it.iconLocked).withHost(host).toString(),
            iconUnlocked = Uri.of(it.iconUnlocked).withHost(host).toString()
        )
    }

    private fun GameDtoV1.withImageHost(host: Uri) = copy(
        displayImage = Uri.of(displayImage).withHost(host).toString()
    )
}