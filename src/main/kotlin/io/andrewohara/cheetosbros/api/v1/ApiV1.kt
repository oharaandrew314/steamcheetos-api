package io.andrewohara.cheetosbros.api.v1

import io.andrewohara.cheetosbros.api.auth.AuthService
import io.andrewohara.cheetosbros.games.GameService
import io.andrewohara.cheetosbros.jobs.JobService
import io.andrewohara.cheetosbros.sync.SyncService
import org.http4k.contract.ContractRoute
import org.http4k.contract.Tag
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.contract.security.Security
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.http4k.lens.*

class ApiV1(
    private val gameService: GameService,
    private val jobsService: JobService,
    private val authService: AuthService,
    private val syncService: SyncService,
    private val authLens: RequestContextLens<String>,
    private val apiSecurity: Security
) {

    object Lenses {
        val gameId = Path.nonEmptyString().of("game_id")
        val clientCallback = Path.base64().of("client_callback")

        val redirectUri = Query.uri().required("redirect_uri")

        val gamesList = Body.auto<List<GameDtoV1>>().toLens()
        val game = Body.auto<GameDtoV1>().toLens()
        val achievementList = Body.auto<List<AchievementDtoV1>>().toLens()
        val jobStatus = Body.auto<JobStatusDtoV1>().toLens()
        val user = Body.auto<UserDtoV1>().toLens()
        val text = Body.nonEmptyString(ContentType.TEXT_PLAIN).toLens()
    }

    object Tags {
        val games = Tag("Games")
        val sync = Tag("Sync")
        val auth = Tag("Auth")
        val profile = Tag("Profile")
    }

    // games

    private val listGamesHandler: HttpHandler = { request ->
        val userId = authLens(request)
        val games = gameService.listGames(userId)

        Response(Status.OK)
            .with(Lenses.gamesList of games.map { it.toDtoV1() })
    }

//    private inner class GetGameHandler(val gameId: String): HttpHandler {
//        override fun invoke(request: Request): Response {
//            val user = authLens(request)
//
//            return gameService.getGame(user, gameId)
//                ?.let { Response(Status.OK).with(Lenses.game of it.toDtoV1()) }
//                ?: Response(Status.NOT_FOUND).body("Could not find game $gameId")
//        }
//    }

    private inner class ListAchievementsHandler(val gameId: String, unused: String): HttpHandler {
        override fun invoke(request: Request): Response {
            val userId = authLens(request)

            val achievements = gameService.listAchievements(userId, gameId)

            return Response(Status.OK).with(Lenses.achievementList of achievements.map { it.toDtoV1() })
        }
    }

    // sync

    inner class SyncGameHandler(val gameId: String) : HttpHandler {
        override fun invoke(request: Request): Response {
            val userId = authLens(request)

            jobsService.startSyncJob(userId, gameId)

            return Response(Status.OK)
        }
    }

    private val syncGamesHandler: HttpHandler = {request ->
        val userId = authLens(request)

        jobsService.startSync(userId)

        Response(Status.OK)
    }

    private val countJobsHandler: HttpHandler = { request ->
        val userId = authLens(request)

        val count = jobsService.countJobsInProgress(userId)

        val dto = JobStatusDtoV1(count = count)

        Response(Status.OK).with(Lenses.jobStatus of dto)
    }

    // openid

    private val getLoginUrl: ContractRoute = "/v1/auth/login" meta {
        operationId = "getLoginUrlV1"
        summary = "Get Login URL"
        tags += Tags.auth
        queries += Lenses.redirectUri
        returning(Status.OK, Lenses.text to "login_url")
    } bindContract Method.GET to { request ->
        val redirectUri = Lenses.redirectUri(request)
        val loginUrl = authService.getLoginUri(redirectUri)

        Response(Status.OK).body(loginUrl.toString())
    }

    private val authCallback: ContractRoute = "/v1/auth/callback" / Lenses.clientCallback meta {
        operationId = "authCallbackV1"
        summary = "Process Openid Login from steam"
        tags += Tags.auth
        returning(Status.OK, Lenses.text to "access_token")
        returning(
            Status.UNAUTHORIZED to "Login failed"
        )
    } bindContract Method.GET to { _ ->
        { request ->
            authService.callback(request)
                ?.let { uri -> Response(Status.FOUND).with(Header.LOCATION of uri) }
                ?: Response(Status.UNAUTHORIZED)
        }
    }

    // user

    private val getUser: ContractRoute = "/v1/user" meta {
        operationId = "getUserDataV1"
        summary = "Get User Data"
        security = apiSecurity
        tags += Tags.profile
        returning(Status.OK, Lenses.user to Examples.user)
        returning(Status.NO_CONTENT to "Could not get user details")
    } bindContract Method.GET to { request ->
        val userId = authLens(request)
        syncService.getUser(userId)
            ?.let { Response(Status.OK).with(Lenses.user of it.toDtoV1()) }
            ?: Response(Status.NO_CONTENT)
    }

    fun routes(): List<ContractRoute> = listOf(
        "/v1/games" meta  {
            summary = "List Games"
            description = "List games in library"
            tags += Tags.games
            returning(Status.UNAUTHORIZED)
            returning(Status.NOT_FOUND)
            returning(Status.OK, Lenses.gamesList to listOf(Examples.game))
            security = apiSecurity
        } bindContract Method.GET to listGamesHandler,
//        "/v1/games/steam" / Lenses.gameId meta {
//            summary = "Get Game"
//            description = "Get game in library by id"
//            tags += Tag("games")
//            returning(Status.UNAUTHORIZED)
//            returning(Status.NOT_FOUND)
//            returning(Status.OK, Lenses.game to Examples.game)
//            security = apiSecurity
//        } bindContract Method.GET to ::GetGameHandler,
        "/v1/games" / Lenses.gameId / "achievements" meta {
            summary = "List Achievements"
            description = "List achievements for game in library"
            tags += Tags.games
            returning(Status.UNAUTHORIZED)
            returning(Status.NOT_FOUND)
            returning(Status.OK, Lenses.achievementList to listOf(Examples.achievement))
            security = apiSecurity
        } bindContract Method.GET to ::ListAchievementsHandler,
        "/v1/sync" meta {
            summary = "Sync"
            description = "discover games in library, and sync achievements for all of them"
            tags += Tags.sync
            returning(Status.UNAUTHORIZED)
            returning(Status.OK)
            security = apiSecurity
        } bindContract Method.POST to syncGamesHandler,
        "/v1/sync" meta {
            summary = "Count Jobs"
            description = "Return the number of jobs in progress for this user"
            tags += Tags.sync
            returning(Status.UNAUTHORIZED)
            returning(Status.OK, Lenses.jobStatus to Examples.jobStatus)
            security = apiSecurity
        } bindContract Method.GET to countJobsHandler,
        "/v1/sync" / Lenses.gameId meta {
            summary = "Sync Game"
            description = "sync a single game in the user's library"
            tags += Tags.sync
            returning(Status.UNAUTHORIZED)
            returning(Status.OK)
            security = apiSecurity
        } bindContract Method.POST to ::SyncGameHandler,
        getLoginUrl,
        authCallback,
        getUser
    )
}