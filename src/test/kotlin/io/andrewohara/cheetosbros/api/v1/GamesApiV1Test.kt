package io.andrewohara.cheetosbros.api.v1

import io.andrewohara.cheetosbros.TestDriver
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.sources.Platform
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import spark.Spark
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class GamesApiV1Test {

    @Rule @JvmField val driver = TestDriver

    private val client = HttpClient.newHttpClient()

    private val gamesMapper = GamesApiV1.JsonMapper.moshi.adapter(Array<OwnedGameDetailsDtoV1>::class.java)

    @Test
    fun `list games`() {
        val user = driver.saveUser(steam = true)
        val player = user.players.getValue(Platform.Steam)

        val game = driver.saveGame(Platform.Steam, 3)
        driver.saveToLibrary(player, game, 1)

        val request = HttpRequest
            .newBuilder(URI.create("http://localhost:${Spark.port()}/v1/games"))
            .asUser(user)
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        assertThat(response.statusCode()).isEqualTo(200)
        assertThat(gamesMapper.fromJson(response.body())).containsExactly(
            OwnedGameDetailsDtoV1(
                uid = game.uid,
                name = game.name,
                achievementsCurrent = 1,
                achievementsTotal = 3,
                displayImage = null,
                lastUpdated = driver.time
            )
        )
    }

    private fun HttpRequest.Builder.asUser(user: User): HttpRequest.Builder {
        return header("Authorization", "Bearer ${driver.authorizationDao.assignToken(user)}")
    }
}