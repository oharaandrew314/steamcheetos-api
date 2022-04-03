package io.andrewohara.cheetosbros.sync

import io.andrewohara.cheetosbros.sources.AchievementData
import io.andrewohara.cheetosbros.sources.AchievementStatusData
import io.andrewohara.cheetosbros.sources.GameData
import io.andrewohara.cheetosbros.sources.UserData
import io.andrewohara.cheetosbros.sources.steam.*
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

    private fun getOwnedGames(request: Request): Response {
        val steamId = SteamClient.Lenses.steamId(request)

        val results = userGames
            .filter { it.first == steamId }
            .map { (_, gameId) -> games.getValue(gameId) }
            .map { game ->
                GetOwnedGamesResponse.Data.Game(
                    appid = game.id.toInt(),
//                    img_logo_url = game.displayImage!!,
//                    name = game.name
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

    private fun getGameData(request: Request): Response {
        val appIds = SteamClient.Lenses.appIds(request)
            .split(",")
            .map { it.toLong() }

        val results = appIds
            .mapNotNull { games[it] }
            .associate { game ->
                game.id to StoreApiGameEntry(
                    success = true,
                    data = StoreApiGameEntry.Data(
                        steam_appid = game.id.toInt(),
                        name = game.name,
                        type = "game",
                        header_image = game.displayImage ?: "no image"
                    )
                )
            }

        return Response(Status.OK).with(SteamClient.Lenses.storeGames of results)
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
                    icon = achievement.iconUnlocked?.toString() ?: "icon",
                    icongray = achievement.iconUnlocked?.toString() ?: "iconGray"
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

    fun createGame(name: String? = null): GameData {
        val id = nextId++.toString()
        val game = GameData(
            id = id,
            displayImage = null,
            name = name ?: "game-$id",
        )
        games[game.id.toLong()] = game

        return game
    }

    fun createAchievement(gameData: GameData, name: String? = null, description: String? = null, hidden: Boolean = false, score: Int? = null): AchievementData {
        val id = nextId++.toString()
        val actualName = name ?: "${gameData.name}-achievement-$id"

        val achievement = AchievementData(
            gameId = gameData.id,
            id = id,
            name = actualName,
            description = description ?: "description for $actualName",
            hidden = hidden,
            iconLocked = null,
            iconUnlocked = null,
            score = score
        )
        achievements += achievement.gameId.toLong() to achievement

        return achievement
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
        SteamClient.Paths.gameData bind Method.GET to ::getGameData,
        SteamClient.Paths.gameSchema bind Method.GET to ::getSchemaForGame,
        SteamClient.Paths.userAchievements bind Method.GET to ::userAchievements,
        SteamClient.Paths.players bind Method.GET to ::getPlayers
    )

    override fun invoke(request: Request) = routes(request)
}