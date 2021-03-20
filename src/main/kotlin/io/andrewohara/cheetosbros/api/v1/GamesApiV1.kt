package io.andrewohara.cheetosbros.api.v1

import io.andrewohara.cheetosbros.api.games.GamesManager
import io.andrewohara.cheetosbros.api.games.Uid
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.sources.JobService
import io.andrewohara.cheetosbros.sources.Platform
import org.http4k.core.*
import org.http4k.lens.RequestContextLens
import java.lang.IllegalArgumentException
import java.time.Instant
import org.http4k.format.Moshi.auto
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST

class GamesApiV1(
    private val gamesManager: GamesManager,
    private val jobService: JobService,
    private val authLens: RequestContextLens<User?>,
    private val time: () -> Instant
) {
    private val mapper = DtoMapperImpl()

    private val gameLens = Body.auto<OwnedGameDetailsDtoV1>().toLens()
    private val achievementsLens = Body.auto<Array<AchievementDetailsDtoV1>>().toLens()
    private val gamesLens = Body.auto<Array<OwnedGameDetailsDtoV1>>().toLens()
    private val jobStatusLens = Body.auto<JobStatusDtoV1>().toLens()

    fun getRoutes() = routes(
        "/v1/games" bind GET to ::listGames,
        "/v1/games/{platform}/{game_id}" bind GET to ::getGame,
        "/v1/games/{platform}/{game_id}/achievements" bind GET to ::listAchievements,

        "/v1/sync" bind POST to ::sync,
        "/v1/sync" bind GET to ::countJobs,
        "/v1/sync/{platform}/{game_id}" bind POST to ::syncGame
    )

    private fun listGames(request: Request): Response {
        val user = authLens(request) ?: return Response(UNAUTHORIZED)

        val games = gamesManager.listGames(user)

        val body = games.map { mapper.toDtoV1(it) }

        return Response(OK).with(gamesLens of body.toTypedArray())
    }

    private fun getGame(request: Request ): Response {
        val user = authLens(request) ?: return Response(UNAUTHORIZED)

        val platform = request.path("platform")?.toPlatform() ?: return Response(BAD_REQUEST, "invalid platform")
        val gameId = request.path("game_id")!!

        val player = user.players[platform]
            ?: return Response(NOT_FOUND).body("User does not have a $platform player")

        val gameDetails = gamesManager.getGame(player, gameId)
            ?: return Response(NOT_FOUND).body("Could not find game $gameId")

        val dto = mapper.toDtoV1(gameDetails)
        return Response(OK).with(gameLens of dto)
    }

    private fun listAchievements(request: Request): Response {
        val user = authLens(request) ?: return Response(UNAUTHORIZED)

        val platform = request.path("platform")?.toPlatform() ?: return Response(BAD_REQUEST, "invalid platform")
        val gameId = request.path("game_id")!!
        val gameUid = Uid(platform, gameId)

        val achievements = gamesManager.listAchievements(user, gameUid)
            ?: return Response(NOT_FOUND).body("Could not find game $gameUid")

        val dto = achievements.map { mapper.toDtoV1(it) }.toTypedArray()
        return Response(OK).with(achievementsLens of dto)
    }

    private fun sync(request: Request): Response {
        val user = authLens(request) ?: return Response(UNAUTHORIZED)

        for (player in user.players.values) {
            jobService.insertDiscoveryJob(user, player, time())
        }

        return Response(OK)
    }

    private fun syncGame(request: Request): Response {
        val user = authLens(request) ?: return Response(UNAUTHORIZED)
        val platform = request.path("platform")?.toPlatform() ?: return Response(BAD_REQUEST, "invalid platform")
        val gameId = request.path("game_id")!!
        val gameUid = Uid(platform, gameId)

        jobService.insertSyncGameJob(user, gameUid, time())

        return Response(OK)
    }

    private fun countJobs(request: Request): Response {
        val user = authLens(request) ?: return Response(UNAUTHORIZED)

        val count = jobService.countJobsInProgress(user.id)

        val dto = JobStatusDtoV1(count = count)
        return Response(OK).with(jobStatusLens of dto)
    }

    private fun String.toPlatform() = try {
        Platform.valueOf(this)
    } catch (e: IllegalArgumentException) {
        null
    }
}