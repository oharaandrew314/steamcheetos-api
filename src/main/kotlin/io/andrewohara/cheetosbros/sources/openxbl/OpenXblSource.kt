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
    override val platform = Platform.Xbox

    override fun getPlayer(playerId: String): Player? {
        val request = HttpRequest.newBuilder()
                .uri(URI.create("$host/account/$playerId"))
                .header("X-Authorization", apiKey)
                .header("X-Contract", "100")
                .GET()
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw IllegalStateException("Request failed: $response")
        }

        val profile = mapper.readValue(response.body(), GetAccountResponse::class.java)
                .profileUsers
                .firstOrNull()
                ?: return null

        return Player(
                id = profile.id,
                platform = platform,
                username = profile.getGamertag()!!,
                avatar = profile.getAvatar()
        )
    }

    override fun library(playerId: String): Collection<Game> {
        val request = HttpRequest.newBuilder()
                .uri(URI.create("$host/achievements/player/$playerId"))
                .header("X-Authorization", apiKey)
                .header("X-Contract", "100")
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
                .map {game -> Game(
                        id = game.titleId,
                        name = game.name,
                        platform = platform,
                        displayImage = game.displayImage
                )}
    }

    override fun achievements(gameId: String): Collection<Achievement> {
        val request = HttpRequest.newBuilder()
                .uri(URI.create("$host/achievements/title/$gameId"))
                .header("X-Authorization", apiKey)
                .header("X-Contract", "100")
                .GET()
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw IllegalStateException("Request failed: $response")
        }

        return mapper.readValue(response.body(), ListAchievementsResponse::class.java)
                .achievements
                .map { Achievement(
                        id = it.id, name = it.name, description = it.lockedDescription, hidden = it.isSecret,
                        icons = it.mediaAssets.filter { asset -> asset.type == "Icon" }.map { asset -> asset.url },
                        score = it.rewards.firstOrNull { reward -> reward.type == "Gamerscore" }?.value?.toIntOrNull()
                ) }
    }

    override fun userAchievements(gameId: String, playerId: String): Collection<AchievementStatus> {
        val request = HttpRequest.newBuilder()
                .uri(URI.create("$host/achievements/player/$playerId/title/$gameId"))
                .header("X-Authorization", apiKey)
                .header("X-Contract", "100")
                .GET()
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw IllegalStateException("Request failed: $response")
        }

        return mapper.readValue(response.body(), ListAchievementsResponse::class.java)
                .achievements
                .map { AchievementStatus(
                        achievementId = it.id,
                        unlockedOn = if (it.progressState == "Achieved") it.progression.timeUnlocked else null
                ) }
    }

    override fun getFriends(playerId: String): Collection<String> {
        val request = HttpRequest.newBuilder()
                .uri(URI.create("$host/friends"))
                .header("X-Authorization", apiKey)
                .header("X-Contract", "100")
                .GET()
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw IllegalStateException("Request failed: $response")
        }

        return mapper.readValue(response.body(), ListFriendsResponse::class.java)
                .people
                .map { it.xuid }
    }
}