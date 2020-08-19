package io.andrewohara.cheetosbros.sources.openxbl

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.andrewohara.cheetosbros.sources.*
import java.lang.IllegalStateException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * https://xbl.io/
 */
class OpenXblSource(private val apiKey: String): Source {
    companion object {

        private const val host = "https://xbl.io/api/v2"
        private val mapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    private val client = HttpClient.newHttpClient()

    override fun resolveUserId(username: String): String? {
        val request = HttpRequest.newBuilder()
                .uri(URI.create("$host/friends/search?gt=$username"))
                .header("X-Authorization", apiKey)
                .GET()
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw IllegalStateException("Request failed: $response")
        }

        if ("\"code\":28" in response.body()) {
            return null
        }

        val user = mapper.readValue(response.body(), SearchFriendResponse::class.java)
                .profileUsers
                .firstOrNull { it.settings.any { setting -> setting.id == "Gamertag" && setting.value.equals(username, ignoreCase = true) } }
                ?: return null

        return user.id
    }

    override fun games(userId: String): Collection<Game> {
        val request = HttpRequest.newBuilder()
                .uri(URI.create("$host/achievements/player/$userId"))
                .header("X-Authorization", apiKey)
                .GET()
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw IllegalStateException("Request failed: $response")
        }

        return mapper.readValue(response.body(), ListGamesResponse::class.java)
                .titles
                .filter { it.type == "Game" }
                .filter { it.achievement.totalGamerscore > 0 }
                .map { Game(id = it.titleId, name = it.name, platform = Game.Platform.Xbox, displayImage = it.displayImage, icon = null) }
    }

    override fun achievements(appId: String): Collection<Achievement> {
        val request = HttpRequest.newBuilder()
                .uri(URI.create("$host/achievements/title/$appId"))
                .header("X-Authorization", apiKey)
                .GET()
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw IllegalStateException("Request failed: $response")
        }

        return mapper.readValue(response.body(), ListAchievementsResponse::class.java)
                .achievements
                .map { Achievement(
                        gameId = appId,
                        id = it.id, name = it.name, description = it.lockedDescription, hidden = it.isSecret,
                        icons = it.mediaAssets.filter { asset -> asset.type == "Icon" }.map { asset -> asset.url }
                ) }

    }

    override fun userAchievements(appId: String, userId: String): Collection<AchievementStatus> {
        val request = HttpRequest.newBuilder()
                .uri(URI.create("$host/achievements/player/$userId/title/$appId"))
                .header("X-Authorization", apiKey)
                .GET()
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw IllegalStateException("Request failed: $response")
        }

        return mapper.readValue(response.body(), ListAchievementsResponse::class.java)
                .achievements
                .map { AchievementStatus(
                        id = it.id,
                        unlockedOn = if (it.progressState == "Achieved") it.progression.timeUnlocked else null
                ) }
    }
}