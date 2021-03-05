package io.andrewohara.cheetosbros.sources.steam

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.andrewohara.cheetosbros.api.games.Uid
import io.andrewohara.cheetosbros.sources.*
import java.io.IOException
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

        private val mapper = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    override val platform = Platform.Steam

    private val client = HttpClient.newHttpClient()

    override fun library(playerId: String): Collection<Source.Game> {
        val request = HttpRequest.newBuilder()
                .uri("IPlayerService","GetOwnedGames", 1, steamId=playerId.toLong(), params = mapOf("include_appinfo" to "1", "include_played_free_games" to "1"))
                .GET()
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw IllegalStateException("Request failed: $response")
        }

        return mapper.adapter(GetOwnedGamesResponse::class.java).fromJson(response.body())!!
                .response
                .games
                .map { game -> Source.Game(
                        uid = Uid(platform, game.appid.toString()),
                        name = game.name,
                        displayImage = "http://media.steampowered.com/steamcommunity/public/images/apps/${game.appid}/${game.img_logo_url}.jpg"
                )}
    }

    override fun achievements(gameId: String): Collection<Achievement> {
        val request = HttpRequest.newBuilder()
                .uri("ISteamUserStats","GetSchemaForGame", 2, params = mapOf("appid" to gameId))
                .GET()
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw IllegalStateException("Request failed: $response")
        }

        return mapper.adapter(GetSchemaForGameResponse::class.java).fromJson(response.body())!!
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

    override fun userAchievements(gameId: String, playerId: String): Collection<AchievementStatus> {
        val request = HttpRequest.newBuilder()
                .uri("ISteamUserStats","GetPlayerAchievements", 1, steamId = playerId.toLong(), params = mapOf("appid" to gameId))
                .GET()
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            if (response.body().contains("Requested app has no stats")) return emptyList()
            throw IllegalStateException("Request failed: $response")
        }

        return mapper.adapter(GetPlayerAchievementsResponse::class.java).fromJson(response.body())!!
                .playerstats
                .achievements
                ?.map { AchievementStatus(achievementId = it.apiname, unlockedOn = if (it.unlocktime > 0) Instant.ofEpochSecond(it.unlocktime) else null) }
                ?: emptyList()
    }

    override fun getPlayer(playerId: String): Player? {
        val request = HttpRequest.newBuilder()
                .uri("ISteamUser","GetPlayerSummaries", 2, params = mapOf("steamids" to playerId))
                .GET()
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        when (response.statusCode()) {
            200 -> {}
            403 -> throw SourceAccessDenied("Could not retrieve achievement for player $playerId.  Profile is not public")
            else -> throw IOException("Request failed: $response")
        }

        val responseBody =  mapper.adapter(GetPlayerSummariesResponse::class.java).fromJson(response.body())!!

        return responseBody
                .response
                .players
                .firstOrNull { it.steamid == playerId }
                ?.let {
                    Player(
                            uid = Uid(platform, it.steamid),
                            username = it.personaname,
                            avatar = it.avatarfull,
                            token = null
                    )
                }
    }

    override fun getFriends(playerId: String): Collection<String> {
        val request = HttpRequest.newBuilder()
                .uri("ISteamUser","GetFriendList", 1, steamId = playerId.toLong(), params = mapOf("relationship" to "friend"))
                .GET()
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw IllegalStateException("Request failed: $response")
        }

        val responseBody = mapper.adapter(GetFriendListResponse::class.java).fromJson(response.body())!!

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

