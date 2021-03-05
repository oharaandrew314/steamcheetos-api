package io.andrewohara.cheetosbros.api.v1

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.andrewohara.cheetosbros.api.games.GamesManager
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.lib.IsoInstantJsonAdapter
import io.andrewohara.cheetosbros.sources.Platform
import io.andrewohara.cheetosbros.sync.SyncClient
import org.eclipse.jetty.util.Promise
import spark.Request
import spark.Response
import spark.ResponseTransformer
import spark.Spark.*
import java.lang.IllegalArgumentException

class GamesApiV1(
    private val gamesManager: GamesManager,
    private val syncClient: SyncClient
) {

    private val mapper = DtoMapperImpl()

    init {
        // games
        get("/v1/games", ::listGames, JsonMapper)
        get("/v1/games/:platform/:game_id", ::getGame, JsonMapper)
        get("/v1/games/:platform/:game_id/achievements", ::listAchievements, JsonMapper)

        // sync
        post("/v1/sync", ::sync)
    }

    object JsonMapper : ResponseTransformer {
        val moshi: Moshi = Moshi.Builder()
            .add(IsoInstantJsonAdapter())
            .addLast(KotlinJsonAdapterFactory())
            .build()

        private val mapper: JsonAdapter<Any> = moshi.adapter<Any>(Object::class.java)

        override fun render(model: Any): String {
            return mapper.toJson(model)
        }
    }

    private fun listGames(request: Request, response: Response): Collection<OwnedGameDetailsDtoV1> {
        val user = request.attribute<User>("user") ?: throw halt(401)

        val games = gamesManager.listGames(user)

        return games.map { mapper.toDtoV1(it) }
    }

    private fun getGame(request: Request, response: Response): OwnedGameDetailsDtoV1 {
        val user = request.attribute<User>("user") ?: throw halt(401)
        val platform = request.params("platform").toPlatform()
        val gameId = request.params("game_id")

        val player = user.players[platform]
            ?: throw halt(404, "User does not have a $platform player")

        val gameDetails = gamesManager.getGame(player, gameId)
            ?: throw halt(404, "Could not find game $gameId")

        return mapper.toDtoV1(gameDetails)
    }

    private fun listAchievements(request: Request, response: Response): Collection<AchievementDetailsDtoV1> {
        val user = request.attribute<User>("user") ?: throw halt(401)
        val platform = request.params("platform").toPlatform()
        val gameId = request.params("game_id")

        val achievements = gamesManager.listAchievements(user, platform, gameId) ?: throw halt(404)

        return achievements.map { mapper.toDtoV1(it) }
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