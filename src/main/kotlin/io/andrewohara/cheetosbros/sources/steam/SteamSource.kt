package io.andrewohara.cheetosbros.sources.steam

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.andrewohara.cheetosbros.sources.*
import java.lang.IllegalStateException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant

/**
 * Schema available at https://partner.steamgames.com/doc/webapi
 */
class SteamSource(private val apiKey: String): Source {
    companion object {
        private const val host = "https://api.steampowered.com"
        private val mapper = jacksonObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    private val client = HttpClient.newHttpClient()

    override fun games(userId: String): Collection<Game> {
        val request = HttpRequest.newBuilder()
                .uri("IPlayerService","GetOwnedGames", 1, steamId=userId.toLong(), params = mapOf("include_appinfo" to "1", "include_played_free_games" to "1"))
                .GET()
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw IllegalStateException("Request failed: $response")
        }

        return mapper.readValue(response.body(), GetOwnedGamesResponse::class.java)
                .response
                .games
                .map { Game(id = it.appid.toString(), name = it.name, platform = Game.Platform.Steam, displayImage = it.img_logo_url, icon = it.img_icon_url) }
    }

    override fun achievements(appId: String): Collection<Achievement> {
        val request = HttpRequest.newBuilder()
                .uri("ISteamUserStats","GetSchemaForGame", 2, params = mapOf("appid" to appId))
                .GET()
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw IllegalStateException("Request failed: $response")
        }

        return mapper.readValue(response.body(), GetSchemaForGameResponse::class.java)
                .game
                .availableGameStats
                ?.achievements
                ?.map { Achievement(
                        id = it.name,
                        name = it.displayName,
                        hidden = it.hidden == 1,
                        icons = listOf(it.icon, it.icongray),
                        description = it.description,
                        score = null
                ) }
                ?: emptySet()
    }

    override fun userAchievements(appId: String, userId: String): Collection<AchievementStatus> {
        val request = HttpRequest.newBuilder()
                .uri("ISteamUserStats","GetPlayerAchievements", 1, steamId = userId.toLong(), params = mapOf("appid" to appId))
                .GET()
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            if (response.body().contains("Requested app has no stats")) return emptyList()
            throw IllegalStateException("Request failed: $response")
        }

        return mapper.readValue(response.body(), GetPlayerAchievementsResponse::class.java)
                .playerstats
                .achievements
                ?.map { AchievementStatus(achievementId = it.apiname, unlockedOn = if (it.unlocktime > 0) Instant.ofEpochSecond(it.unlocktime) else null) }
                ?: emptyList()
    }

    override fun resolveUserId(username: String): String? {
        val request = HttpRequest.newBuilder()
                .uri("ISteamUser","ResolveVanityURL", 1, params = mapOf("vanityurl" to username))
                .GET()
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw IllegalStateException("Request failed: $response")
        }

        return mapper.readValue(response.body(), ResolveVanityURLResponse::class.java)
                .response
                .let { if (it.success == 1) it.steamid!! else null }
    }

    override fun getPlayer(id: String): Player? {
        val request = HttpRequest.newBuilder()
                .uri("ISteamUser","GetPlayerSummaries", 2, params = mapOf("steamids" to id))
                .GET()
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw IllegalStateException("Request failed: $response")
        }

        val responseBody =  mapper.readValue(response.body(), GetPlayerSummariesResponse::class.java)

        return responseBody
                .response
                .players
                .firstOrNull { it.steamid == id }
                ?.let {
                    Player(
                            id = it.steamid,
                            platform = Game.Platform.Steam,
                            username = it.personaname,
                            avatar = it.avatarfull
                    )
                }
    }

    override fun getFriends(userId: String): Collection<String> {
        val request = HttpRequest.newBuilder()
                .uri("ISteamUser","GetFriendList", 1, steamId = userId.toLong(), params = mapOf("relationship" to "friend"))
                .GET()
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw IllegalStateException("Request failed: $response")
        }

        val responseBody = mapper.readValue(response.body(), GetFriendListResponse::class.java)

        return responseBody.friendslist.friends.map { it.steamid }
    }

    private fun HttpRequest.Builder.uri(service: String, method: String, version: Int, steamId: Long? = null, params: Map<String, String> = emptyMap()): HttpRequest.Builder {
        val fullParams = params.toMutableMap().apply {
            put("key", apiKey)
            if (steamId != null) put("steamid", steamId.toString())
        }
        val queryString = fullParams.map { "${it.key}=${it.value}" }
                .joinToString("&")

        return uri(URI.create("$host/$service/$method/V$version?$queryString"))
    }
}

