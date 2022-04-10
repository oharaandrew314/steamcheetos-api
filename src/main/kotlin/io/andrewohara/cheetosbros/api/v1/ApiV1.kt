package io.andrewohara.cheetosbros.api.v1

import io.andrewohara.cheetosbros.api.auth.AuthService
import io.andrewohara.cheetosbros.CheetosService
import org.http4k.contract.ContractRoute
import org.http4k.contract.Tag
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.contract.security.Security
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.http4k.lens.*

class ApiV1(
    private val service: CheetosService,
    private val auth: AuthService,
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
        val user = Body.auto<UserDtoV1>().toLens()
        val text = Body.nonEmptyString(ContentType.TEXT_PLAIN).toLens()
    }

    object Tags {
        val games = Tag("Games")
        val auth = Tag("Auth")
        val profile = Tag("Profile")
    }

    // games

    private val listGames: ContractRoute = "/v1/games" meta {
        operationId = "listGamesV1"
        summary = "List Games"
        description = "List games in library"
        tags += Tags.games
        returning(Status.UNAUTHORIZED to "unauthorized user", Status.NOT_FOUND to "user not found")
        returning(Status.OK, Lenses.gamesList to listOf(Examples.game))
        security = apiSecurity
    } bindContract Method.GET to { ->
        { request ->
            val userId = authLens(request)
            val results = service.listGames(userId).map { it.toDtoV1() }

            Response(Status.OK).with(Lenses.gamesList of results)
        }
    }

    private val refreshGames: ContractRoute = "/v1/games" meta {
        operationId = "refreshGamesV1"
        summary = "Refresh Games"
        description = "Reload games from steam.  Newly loaded games will not have their achievements synced"
        tags += Tags.games
        returning(Status.UNAUTHORIZED to "unauthorized user", Status.NOT_FOUND to "user not found")
        returning(Status.OK, Lenses.gamesList to listOf(Examples.game))
        security = apiSecurity
    } bindContract Method.POST to { ->
        { request ->
            val userId = authLens(request)
            val results = service.refreshGames(userId).map { it.toDtoV1() }

            Response(Status.OK).with(Lenses.gamesList of results)
        }
    }

    private val listAchievements: ContractRoute = "/v1/games" / Lenses.gameId / "achievements" meta {
        operationId = "listAchievementsV1"
        summary = "List Achievements"
        description = "List achievements for game in library"
        tags += Tags.games
        returning(Status.UNAUTHORIZED to "unauthorized user")
        returning(Status.OK, Lenses.achievementList to listOf(Examples.achievement))
        security = apiSecurity
    } bindContract Method.GET to { gameId, _ ->
        { request ->
            val userId = authLens(request)
            val results = service.listAchievements(userId, gameId).toDtoV1s()
            Response(Status.OK).with(Lenses.achievementList of results)
        }
    }

    private val refreshAchievements: ContractRoute = "/v1/games" / Lenses.gameId / "achievements" meta {
        operationId = "refreshAchievementsV1"
        summary = "Refresh Achievements"
        description = "Load achievements for game from steam"
        tags += Tags.games
        returning(Status.UNAUTHORIZED to "unauthorized user", Status.NOT_FOUND to "game not found")
        returning(Status.OK, Lenses.achievementList to listOf(Examples.achievement))
        security = apiSecurity
    } bindContract Method.POST to { gameId, _ ->
        { request ->
            val userId = authLens(request)

            service.refreshAchievements(userId, gameId)
                ?.let { Response(Status.OK).with(Lenses.achievementList of it.toDtoV1s()) }
                ?: Response(Status.NOT_FOUND)
        }
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
        val loginUrl = auth.getLoginUri(redirectUri)

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
            auth.callback(request)
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
        service.getUser(userId)
            ?.let { Response(Status.OK).with(Lenses.user of it.toDtoV1()) }
            ?: Response(Status.NO_CONTENT)
    }

    fun routes(): List<ContractRoute> = listOf(
        listGames,
        refreshGames,
        getLoginUrl,
        authCallback,
        getUser,
        listAchievements,
        refreshAchievements
    )
}