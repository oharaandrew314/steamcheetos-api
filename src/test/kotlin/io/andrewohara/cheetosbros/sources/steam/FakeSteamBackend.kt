package io.andrewohara.cheetosbros.sources.steam

import io.andrewohara.cheetosbros.sources.AchievementData
import io.andrewohara.cheetosbros.sources.AchievementStatusData
import io.andrewohara.cheetosbros.sources.GameData
import io.andrewohara.cheetosbros.sources.UserData
import org.http4k.core.*
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.Instant

typealias SteamId = Long
typealias AppId = Long

class FakeSteamBackend: HttpHandler {

    private var nextId = 0

    private val players = mutableMapOf<SteamId, UserData>()
    private val games = mutableMapOf<AppId, GameData>()

    private val userGames = mutableSetOf<Pair<SteamId, AppId>>()
    private val achievements = mutableSetOf<Pair<AppId, AchievementData>>()
    private val userAchievements = mutableSetOf<Triple<SteamId, AppId, AchievementStatusData>>()
    private val recentlyPlayed = mutableSetOf<Pair<SteamId, AppId>>()

    private fun getOwnedGames(request: Request): Response {
        val steamId = SteamClient.Lenses.steamId(request)

        val results = userGames
            .filter { it.first == steamId }
            .map { (_, gameId) -> games.getValue(gameId) }
            .map { game ->
                GetOwnedGamesResponse.Data.Game(
                    appid = game.id.toInt(),
                    img_logo_url = game.displayImage.toString(),
                    name = game.name
                )
            }

        val response = GetOwnedGamesResponse(
            response = GetOwnedGamesResponse.Data(
                game_count = results.size,
                games = results
            )
        )

        return Response(Status.OK).with(SteamClient.Lenses.ownedGames of response)
    }

    private fun getSchemaForGame(request: Request): Response {
        val appId = SteamClient.Lenses.appId(request)

        val game = games.getValue(appId)

        val achievements = achievements
            .filter { it.first == appId }
            .map { (_, achievement) ->
                GetSchemaForGameResponse.Content.Data.Achievement(
                    name = achievement.id,
                    displayName = achievement.name,
                    description = achievement.description,
                    defaultvalue = 0,
                    hidden = if (achievement.hidden) 1 else 0,
                    icon = achievement.iconUnlocked.toString(),
                    icongray = achievement.iconUnlocked.toString()
                )
            }

        val response = GetSchemaForGameResponse(
            game = GetSchemaForGameResponse.Content(
                gameName = game.name,
                gameVersion = null,
                availableGameStats = GetSchemaForGameResponse.Content.Data(
                    achievements = achievements,
                    stats = emptyList()
                )
            )
        )

        return Response(Status.OK).with(SteamClient.Lenses.gameSchema of response)
    }

    private fun userAchievements(request: Request): Response {
        val steamId = SteamClient.Lenses.steamId(request)
        val appId = SteamClient.Lenses.appId(request)

        val game = games.getValue(appId)

        val gameAchievements = achievements
            .filter { it.first == appId }
            .map { it.second }

        // only contains completed achievements
        val progress = userAchievements
            .filter { (userId, gameId, _) -> userId == steamId && gameId == appId }
            .map { (_, _, data) ->
                GetPlayerAchievementsResponse.PlayerStats.Achievement(
                    apiname = data.achievementId,
                    achieved = if (data.unlockedOn != null) 1 else 0,
                    unlocktime = data.unlockedOn?.epochSecond ?: 0L
                )
            }
            .associateBy { it.apiname }

        // add incomplete achievements to result
        val fullProgress = gameAchievements
            .map { data ->  progress[data.id] ?: GetPlayerAchievementsResponse.PlayerStats.Achievement(
                apiname = data.id,
                achieved = 0,
                unlocktime = 0
            ) }

        val response = GetPlayerAchievementsResponse(
            playerstats = GetPlayerAchievementsResponse.PlayerStats(
                steamID = steamId,
                gameName = game.name,
                achievements = fullProgress
            )
        )

        return Response(Status.OK).with(SteamClient.Lenses.playerAchievements of response)
    }

    private fun getPlayers(request: Request): Response {
        val playerIds = SteamClient.Lenses.steamIds(request)
            .split(",")
            .map { it.toLong() }

        val players = playerIds
            .mapNotNull { players[it] }
            .map { player ->
                GetPlayerSummariesResponse.Data.PlayerSummary(
                    steamid = player.id,
                    avatarfull = player.avatar?.toString() ?: "avatar",
                    personaname = player.username
                )
            }

        val response = GetPlayerSummariesResponse(
            response = GetPlayerSummariesResponse.Data(
                players = players
            )
        )

        return Response(Status.OK).with(SteamClient.Lenses.playerSummaries of response)
    }

    private fun getRecentlyPlayed(request: Request): Response {
        val steamId = SteamClient.Lenses.steamId(request)
        val count = SteamClient.Lenses.count(request)

        val results = recentlyPlayed
            .filter { it.first == steamId }
            .mapNotNull { games[it.second] }
            .take(count?.takeIf { it != 0 } ?: Int.MAX_VALUE)
            .map { game ->
                GetRecentlyPlayedResponse.Data.Game(
                    appid = game.id.toInt(),
                    name = game.name,
                    img_logo_url = game.displayImage.toString()
                )
            }

        val response = GetRecentlyPlayedResponse(
            response = GetRecentlyPlayedResponse.Data(
                total_count = results.size,
                games = results
            )
        )

        return Response(Status.OK).with(SteamClient.Lenses.recentlyPlayed of response)
    }

    fun createUser(displayName: String, avatar: String? = null): UserData {
        val id = nextId++.toString()
        val user = UserData(
            id = id,
            avatar = avatar?.let { Uri.of(it) },
            username = displayName
        )

        players[user.id.toLong()] = user
        return user
    }

    operator fun plusAssign(data: GameData) {
        games[data.id.toLong()] = data
    }

    operator fun plusAssign(data: AchievementData) {
        achievements += data.gameId.toLong() to data
    }

    operator fun set(userId: String, game: GameData) {
        userGames += userId.toLong() to game.id.toLong()
    }

    fun recentlyPlayed(userId: String, game: GameData) {
        recentlyPlayed += userId.toLong() to game.id.toLong()
    }

    fun unlockAchievement(userId: String, achievement: AchievementData, unlocked: Instant?): AchievementStatusData {
        val status = AchievementStatusData(
            achievementId = achievement.id,
            unlockedOn = unlocked
        )

        userAchievements += Triple(userId.toLong(), achievement.gameId.toLong(), status)

        return status
    }

    private val routes = routes(
        SteamClient.Paths.ownedGames bind Method.GET to ::getOwnedGames,
        SteamClient.Paths.gameSchema bind Method.GET to ::getSchemaForGame,
        SteamClient.Paths.userAchievements bind Method.GET to ::userAchievements,
        SteamClient.Paths.players bind Method.GET to ::getPlayers,
        SteamClient.Paths.recentlyPlayedGames bind Method.GET to ::getRecentlyPlayed,
    )

    override fun invoke(request: Request) = routes(request)
}