package io.andrewohara.cheetosbros.api.v1

import io.andrewohara.cheetosbros.TestDriver
import io.andrewohara.cheetosbros.me3AchievementData
import io.andrewohara.cheetosbros.me3Data
import io.kotest.matchers.be
import io.kotest.matchers.collections.containExactly
import io.kotest.matchers.collections.containExactlyInAnyOrder
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.Uri
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
        response.shouldHaveBody(ApiV1.Lenses.gamesList, containExactly(created.game.toDtoV1()))
    }

    @Test
    fun `list achievements`() {
        val created = driver.createGame(userId, me3Data, me3AchievementData, emptyList())

        val response = Request(Method.GET, "/v1/games/${created.game.id}/achievements")
            .asUser(userId)
            .let(driver)

        response shouldHaveStatus Status.OK
        response.shouldHaveBody(ApiV1.Lenses.achievementList, containExactlyInAnyOrder(created.achievements.toDtoV1s()))
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
}