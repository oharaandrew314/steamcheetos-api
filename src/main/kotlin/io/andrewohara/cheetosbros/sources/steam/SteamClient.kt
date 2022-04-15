package io.andrewohara.cheetosbros.sources.steam

import io.andrewohara.cheetosbros.sources.*
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.http4k.lens.*
import java.io.IOException
import java.lang.IllegalStateException
import java.time.Instant

/**
 * Schema available at https://partner.steamgames.com/doc/webapi
 */
class SteamClient(private val client: HttpHandler) {

    object Lenses {
        val ownedGames = Body.auto<GetOwnedGamesResponse>().toLens()
        val gameSchema = Body.auto<GetSchemaForGameResponse>().toLens()
        val playerAchievements = Body.auto<GetPlayerAchievementsResponse>().toLens()
        val playerSummaries = Body.auto<GetPlayerSummariesResponse>().toLens()
        val friendsList = Body.auto<GetFriendListResponse>().toLens()
        val recentlyPlayed = Body.auto<GetRecentlyPlayedResponse>().toLens()

        val steamIds = Query.string().required("steamids")
        val steamId = Query.long().required("steamid")
//        val appIds = Query.nonEmptyString().required("appids")
        val appId = Query.long().required("appid")
        val count = Query.int().optional("count")
    }

    object Paths {
        const val ownedGames = "IPlayerService/GetOwnedGames/V1"
        const val gameSchema = "ISteamUserStats/GetSchemaForGame/V2"
        const val userAchievements = "ISteamUserStats/GetPlayerAchievements/V1"
        const val players = "ISteamUser/GetPlayerSummaries/V2"
        const val recentlyPlayedGames = "/IPlayerService/GetRecentlyPlayedGames/v1/"
    }

    fun listOwnedGames(playerId: Long): Collection<GameData> {
        val request = Request(Method.GET, Paths.ownedGames)
            .with(Lenses.steamId of playerId)
            .query("include_appinfo", "1")
            .query("include_played_free_games", "1")

        val response = client(request)
        if (!response.status.successful) throw IllegalStateException("Request failed: $response")

        return Lenses.ownedGames(response)
            .response
            .games
            .map { game ->
                GameData(
                    id = game.appid.toString(),
                    name = game.name,
                    displayImage = Uri.of("https://cdn.akamai.steamstatic.com/steam/apps/${game.appid}/header.jpg")
//                    displayImage = Uri.of("http://media.steampowered.com/steamcommunity/public/images/apps/${game.appid}/${game.img_logo_url}.jpg")
                )
            }
    }

    fun listRecentGameIds(playerId: String, limit: Int): Collection<String> {
        val response = Request(Method.GET, Paths.recentlyPlayedGames)
            .with(Lenses.steamId of playerId.toLong())
            .with(Lenses.count of limit)
            .let(client)

        if (!response.status.successful) throw IOException("Request failed: $response")

        return Lenses.recentlyPlayed(response).response.games
            .map { it.appid.toString() }
    }

    fun achievements(gameId: Long): Collection<AchievementData> {
        val response = Request(Method.GET, Paths.gameSchema)
            .with(Lenses.appId of gameId)
            .let(client)

        return when(response.status) {
            Status.FORBIDDEN -> emptySet()
            Status.OK -> {
                Lenses.gameSchema(response)
                    .game
                    .availableGameStats
                    ?.achievements
                    ?.map {
                        AchievementData(
                            gameId = gameId.toString(),
                            id = it.name,
                            name = it.displayName,
                            hidden = it.hidden == 1,
                            iconLocked = Uri.of(it.icongray),
                            iconUnlocked = Uri.of(it.icon),
                            description = it.description,
                        )
                    }
                    ?: emptySet()
            }
            else -> throw IOException("Request failed: $response")
        }
    }

    fun userAchievements(gameId: Long, playerId: Long): Collection<AchievementStatusData> {
        val response = Request(Method.GET, Paths.userAchievements)
            .with(Lenses.steamId of playerId)
            .with(Lenses.appId of gameId)
            .let(client)

        if (!response.status.successful) {
            if (response.bodyString().contains("Requested app has no stats")) return emptyList()
            throw IllegalStateException("Request failed: $response")
        }

        return Lenses.playerAchievements(response)
            .playerstats
            .achievements
            ?.map { AchievementStatusData(achievementId = it.apiname, unlockedOn = if (it.unlocktime > 0) Instant.ofEpochSecond(it.unlocktime) else null) }
            ?: emptyList()
    }

    fun getPlayer(playerId: Long): UserData? {
        val response = Request(Method.GET, Paths.players)
            .with(Lenses.steamIds of playerId.toString())
            .let(client)

        when (response.status) {
            Status.OK -> {}
            Status.FORBIDDEN -> throw SourceAccessDenied("Could not retrieve achievement for player $playerId.  Profile is not public")
            else -> throw IOException("Request failed: $response")
        }

        return Lenses.playerSummaries(response)
            .response
            .players
            .firstOrNull { it.steamid == playerId.toString() }
            ?.let {
                UserData(
                        id = it.steamid,
                        username = it.personaname,
                        avatar = Uri.of(it.avatarfull)
                )
            }
    }

    fun getFriends(playerId: Long): Collection<String> {
        val response = Request(Method.GET, "ISteamUser/GetFriendList/V1")
            .with(Lenses.steamId of playerId)
            .query("relationship", "friend")
            .let(client)

        if (!response.status.successful) throw IllegalStateException("Request failed: $response")

        return Lenses.friendsList(response)
            .friendslist
            .friends
            .map { it.steamid }
    }
}

