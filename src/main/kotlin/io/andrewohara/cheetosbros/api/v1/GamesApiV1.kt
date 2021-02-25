package io.andrewohara.cheetosbros.api.v1

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.andrewohara.cheetosbros.api.games.v1.GamesManager
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.sources.Achievement
import io.andrewohara.cheetosbros.sources.AchievementStatus
import io.andrewohara.cheetosbros.sources.Platform
import io.andrewohara.cheetosbros.sync.SyncClient
import spark.Request
import spark.Response
import spark.ResponseTransformer
import spark.Spark.*
import java.lang.IllegalArgumentException

class GamesApiV1(
    private val gamesManager: GamesManager,
    private val syncClient: SyncClient
) {

    init {
        // games
        get("/v1/games", ::listGames, JsonTransformer)
        get("/v1/games/:platform/:game_id", ::getGame, JsonTransformer)
        get("/v1/games/:platform/:game_id/achievements", ::listAchievements, JsonTransformer)
        get("/v1/games/:platform/:game_id/achievements/status", ::listAchievementStatus, JsonTransformer)

        // sync
        post("/v1/sync", ::sync)
    }

    private object JsonTransformer : ResponseTransformer {
        private val mapper = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
            .adapter<Any>(Object::class.java)

        override fun render(model: Any): String {
            return mapper.toJson(model)
        }
    }

    private fun listGames(request: Request, response: Response): Collection<GameDtoV1> {
        val user = request.attribute<User>("user") ?: throw halt(401)

        val games = gamesManager.listGames(user)

        return games.map { GameDtoV1.create(it) }
    }

    private fun getGame(request: Request, response: Response): GameDtoV1 {
        val user = request.attribute<User>("user") ?: throw halt(401)
        val platform = request.params("platform").toPlatform()
        val gameId = request.params("game_id")

        val player = user.players[platform]
            ?: throw halt(404, "User does not have a $platform player")

        val game = gamesManager.getGame(player, gameId)
            ?: throw halt(404, "Could not find game $gameId")

        return GameDtoV1.create(game)
    }

    private fun listAchievements(request: Request, response: Response): Collection<Achievement> {
        val platform = request.params("platform").toPlatform()
        val gameId = request.params("game_id")

        return gamesManager.listAchievements(platform, gameId) ?: throw halt(404)
    }

    private fun listAchievementStatus(request: Request, response: Response): Collection<AchievementStatus> {
        val user = request.attribute<User>("user") ?: throw halt(401)
        val platform = request.params("platform").toPlatform()
        val gameId = request.params("game_id")

        return gamesManager.listAchievementStatus(user, platform, gameId) ?: throw halt(404)
    }

    private fun sync(request: Request, response: Response) {
        val user = request.attribute<User>("user") ?: throw halt(401)

        for (player in user.players.values) {
            syncClient.sync(player)
        }
    }

    private fun String.toPlatform() = try {
        Platform.valueOf(this)
    } catch (e: IllegalArgumentException) {
        throw halt(404, "Invalid platform: $this")
    }
}