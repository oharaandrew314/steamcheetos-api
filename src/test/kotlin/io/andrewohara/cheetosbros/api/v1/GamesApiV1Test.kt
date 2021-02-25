package io.andrewohara.cheetosbros.api.v1

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.andrewohara.cheetosbros.api.TestDriver
import io.andrewohara.cheetosbros.api.games.v1.OwnedGame
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

    @Rule @JvmField val driver = TestDriver()

    private val client = HttpClient.newHttpClient()
    private val mapper = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val gamesMapper = mapper.adapter(Array<GameDtoV1>::class.java)

    @Test
    fun `list games`() {
        val game = OwnedGame(
            platform = Platform.Steam,
            id = "123",
            name = "Satisfactory",
            currentAchievements = 1,
            displayImage = null,
            totalAchievements = 3
        )

        driver.libraryDao.save(driver.steamPlayer1, game)

        val request = HttpRequest
            .newBuilder(URI.create("http://localhost:${Spark.port()}/v1/games"))
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        assertThat(response.statusCode()).isEqualTo(200)
        assertThat(gamesMapper.fromJson(response.body())).containsExactly(
            GameDtoV1(
                platform = Platform.Steam,
                uid = "Steam-123",
                name = "Satisfactory",
                achievementsCurrent = 1,
                achievementsTotal = 3,
                displayImage = null
            )
        )
    }
}