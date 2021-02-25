package io.andrewohara.cheetosbros.api.v1

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.andrewohara.cheetosbros.api.ApiTestDriver
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

    @Rule @JvmField val driver = ApiTestDriver

    private val client = HttpClient.newHttpClient()
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val gamesMapper = moshi.adapter(Array<GameDtoV1>::class.java)

    @Test
    fun `list games`() {
        val user = driver.createUser(steam = driver.steamPlayer1)
        val game = driver.createGame(Platform.Steam)
        driver.addToLibrary(driver.steamPlayer1, game, 1, 3)

        val request = HttpRequest
            .newBuilder(URI.create("http://localhost:${Spark.port()}/v1/games"))
            .asUser(user)
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        assertThat(response.statusCode()).isEqualTo(200)
        assertThat(gamesMapper.fromJson(response.body())).containsExactly(
            GameDtoV1(
                platform = Platform.Steam,
                uid = game.uid(),
                name = game.name,
                achievementsCurrent = 1,
                achievementsTotal = 3,
                displayImage = null
            )
        )
    }

    private fun HttpRequest.Builder.asUser(user: User): HttpRequest.Builder {
        return header("Authorization", "Bearer ${driver.authorizationDao.assignToken(user)}")
    }
}