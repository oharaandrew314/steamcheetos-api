package io.andrewohara.cheetosbros.sources.openxbl

import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.andrewohara.cheetosbros.sources.*
import java.lang.IllegalStateException
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant

/**
 * https://xbl.io/
 */
class OpenXblSource(private val apiKey: String): Source {
    companion object {

        private const val host = "https://xbl.io/api/v2"
        private val mapper = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .add(object {
                    @FromJson fun fromJson(string: String) = BigDecimal(string)
                    @ToJson fun toJson(value: BigDecimal) = value.toString()
                })
                .add(object {
                    @FromJson fun fromJson(string: String) = Instant.parse(string)
                    @ToJson fun toJson(value: Instant) = value.toString()
                })
                .build()
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

        val user = mapper.adapter(SearchFriendResponse::class.java).fromJson(response.body())!!
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

        return mapper.adapter(ListGamesResponse::class.java).fromJson(response.body())!!
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

        return mapper.adapter(ListAchievementsResponse::class.java).fromJson(response.body())!!
                .achievements
                .map { Achievement(
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

        return mapper.adapter(ListAchievementsResponse::class.java).fromJson(response.body())!!
                .achievements
                .map { AchievementStatus(
                        id = it.id,
                        unlocked = it.progressState == "Achieved",
                        unlockedOn = if (it.progressState == "Achieved") it.progression.timeUnlocked else null
                ) }
    }
}