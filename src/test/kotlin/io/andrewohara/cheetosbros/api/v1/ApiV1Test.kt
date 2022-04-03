package io.andrewohara.cheetosbros.api.v1

import io.andrewohara.cheetosbros.TestDriver
import io.andrewohara.cheetosbros.tnow
import io.kotest.matchers.be
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
        val dsp = driver.createGame(userId, "Dyson Sphere Program")

        val response = Request(Method.GET, "/v1/games")
            .asUser(userId)
            .let(driver)

        response shouldHaveStatus Status.OK
        response.shouldHaveBody(ApiV1.Lenses.gamesList, be(listOf(dsp.game.toDtoV1())))
    }

    @Test
    fun `list achievements`() {
        val btd6 = driver.createGame(
            userId, "Bloons TD 6",
            Triple("Superior Bloons Master", "Beat 5 maps in CHIMPS mode", tnow),
            Triple("Triple Threat", "Beat 1 map in 3-person co-op mode", null)
        )

        val response = Request(Method.GET, "/v1/games/${btd6.game.id}/achievements")
            .asUser(userId)
            .let(driver)

        response shouldHaveStatus Status.OK
        response.shouldHaveBody(ApiV1.Lenses.achievementList, be(btd6.achievements.map { it.toDtoV1() }))
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