package io.andrewohara.cheetosbros.api.v1

import io.andrewohara.cheetosbros.api.auth.AuthService
import io.andrewohara.cheetosbros.CheetosService
import org.http4k.contract.ContractRoute
import org.http4k.contract.Tag
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.contract.security.Security
import org.http4k.core.*
import org.http4k.format.Moshi.auto
import org.http4k.lens.*

class ApiV1(
    private val service: CheetosService,
    private val auth: AuthService,
    private val authLens: RequestContextLens<String>,
    private val apiSecurity: Security
) {

    object Lenses {
        val gameId = Path.nonEmptyString().of("game_id")
        val achievementId = Path.nonEmptyString().of("achievement_id")
        val userId = Path.nonEmptyString().of("user_id")
        val clientCallback = Path.base64().of("client_callback")

        val redirectUri = Query.uri().required("redirect_uri")

        val gamesList = Body.auto<Array<GameDtoV1>>().toLens()
        val game = Body.auto<GameDtoV1>().toLens()
        val achievement = Body.auto<AchievementDtoV1>().toLens()
        val achievementList = Body.auto<Array<AchievementDtoV1>>().toLens()
        val achievementStatusList = Body.auto<Array<AchievementStatusDtoV1>>().toLens()
        val user = Body.auto<UserDtoV1>().toLens()
        val users = Body.auto<Array<UserDtoV1>>().toLens()
        val text = Body.nonEmptyString(ContentType.TEXT_PLAIN).toLens()
        val updateGame = Body.auto<UpdateGameRequestV1>().toLens()
        val updateAchievement = Body.auto<UpdateAchievementRequestV1>().toLens()
    }

    object Tags {
        val games = Tag("Games")
        val auth = Tag("Auth")
        val users = Tag("Users")
    }

    // games

    private val listGames: ContractRoute = "/v1/games" meta {
        operationId = "listGamesV1"
        summary = "List Games"
        description = "List games in library"
        tags += Tags.games
        returning(Status.UNAUTHORIZED to "unauthorized user", Status.NOT_FOUND to "user not found")
        returning(Status.OK, Lenses.gamesList to arrayOf(Examples.game))
        security = apiSecurity
    } bindContract Method.GET to { ->
        { request ->
            val userId = authLens(request)
            val results = service.listGames(userId).map { it.toDtoV1() }.toTypedArray()

            Response(Status.OK).with(Lenses.gamesList of results)
        }
    }

    private val refreshGames: ContractRoute = "/v1/games" meta {
        operationId = "refreshGamesV1"
        summary = "Refresh Games"
        description = "Reload games from steam.  Newly loaded games will not have their achievements synced"
        tags += Tags.games
        returning(Status.UNAUTHORIZED to "unauthorized user", Status.NOT_FOUND to "user not found")
        returning(Status.OK, Lenses.gamesList to arrayOf(Examples.game))
        security = apiSecurity
    } bindContract Method.POST to { ->
        { request ->
            val userId = authLens(request)
            val results = service.refreshGames(userId).map { it.toDtoV1() }.toTypedArray()

            Response(Status.OK).with(Lenses.gamesList of results)
        }
    }

    private val updateGame: ContractRoute = "/v1/games" / Lenses.gameId meta {
        operationId = "updateGameV1"
        summary = "Update Game"
        tags += Tags.games
        receiving(Lenses.updateGame to Examples.updateGame)
        returning(Status.OK, Lenses.game to Examples.game)
        returning(
            Status.UNAUTHORIZED to "unauthorized user",
            Status.NOT_FOUND to "game not found"
        )
        security = apiSecurity
    } bindContract Method.PUT to { game ->
        { request ->
            val userId = authLens(request)
            val data = Lenses.updateGame(request)
            service.updateGame(userId, game, data.favourite)
                ?.let { Response(Status.OK).with(Lenses.game of it.toDtoV1()) }
                ?: Response(Status.NOT_FOUND)
        }
    }

    private val listAchievements: ContractRoute = "/v1/games" / Lenses.gameId / "achievements" meta {
        operationId = "listAchievementsV1"
        summary = "List Achievements"
        description = "List achievements for game in library"
        tags += Tags.games
        returning(Status.UNAUTHORIZED to "unauthorized user")
        returning(Status.OK, Lenses.achievementList to arrayOf(Examples.achievement))
        security = apiSecurity
    } bindContract Method.GET to { gameId, _ ->
        { request ->
            val userId = authLens(request)
            val results = service.listAchievements(userId, gameId).toDtoV1s()
            Response(Status.OK).with(Lenses.achievementList of results)
        }
    }

    private val listAchievementsForFriend: ContractRoute = "v1/games" / Lenses.gameId / "friends" / Lenses.userId / "achievements" meta {
        operationId = "listAchievementsForUser"
        summary = "List Achievements for User"
        tags += Tags.games
        returning(Status.UNAUTHORIZED to "unauthorized user")
        returning(Status.OK, Lenses.achievementStatusList to arrayOf(Examples.achievementStatus))
        security = apiSecurity
    } bindContract Method.GET to { gameId, _, userId, _ ->
        {
            val results = service.listAchievementStatus(userId = userId, gameId = gameId).map { it.toDtoV1() }.toTypedArray()
            Response(Status.OK).with(Lenses.achievementStatusList of results)
        }
    }

    private val refreshAchievements: ContractRoute = "/v1/games" / Lenses.gameId / "achievements" meta {
        operationId = "refreshAchievementsV1"
        summary = "Refresh Achievements"
        description = "Load achievements for game from steam"
        tags += Tags.games
        returning(Status.UNAUTHORIZED to "unauthorized user", Status.NOT_FOUND to "game not found")
        returning(Status.OK, Lenses.achievementList to arrayOf(Examples.achievement))
        security = apiSecurity
    } bindContract Method.POST to { gameId, _ ->
        { request ->
            val userId = authLens(request)

            service.refreshAchievements(userId, gameId)
                ?.let { Response(Status.OK).with(Lenses.achievementList of it.toDtoV1s()) }
                ?: Response(Status.NOT_FOUND)
        }
    }

    private val updateAchievement: ContractRoute = "/v1/games" / Lenses.gameId / "achievements" / Lenses.achievementId meta {
        operationId = "updateAchievementV1"
        summary = "Update Achievement"
        tags += Tags.games
        receiving(Lenses.updateAchievement to Examples.updateAchievement)
        returning(Status.OK, Lenses.achievement to Examples.achievement)
        returning(Status.UNAUTHORIZED to "unauthorized user", Status.NOT_FOUND to "achievement not found")
        security = apiSecurity
    } bindContract Method.PUT to { gameId, _, achievementId ->
        { request ->
            val userId = authLens(request)
            val data = Lenses.updateAchievement(request)

            service.updateAchievement(userId, gameId, achievementId, favourite = data.favourite)
                ?.let { Response(Status.OK).with(Lenses.achievement of it.toDtoV1()) }
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
        tags += Tags.users
        returning(Status.OK, Lenses.user to Examples.user)
        returning(Status.UNAUTHORIZED to "Unauthorized", Status.NO_CONTENT to "Could not get user details")
    } bindContract Method.GET to { request ->
        val userId = authLens(request)
        service.getUser(userId)
            ?.let { Response(Status.OK).with(Lenses.user of it.toDtoV1()) }
            ?: Response(Status.NO_CONTENT)
    }

    private val getFriends: ContractRoute = "/v1/friends" meta {
        operationId = "getFriends"
        summary = "Get Friends"
        security = apiSecurity
        tags += Tags.users
        returning(Status.OK, Lenses.users to arrayOf(Examples.user))
        returning(Status.UNAUTHORIZED to "unauthorized")
    } bindContract Method.GET to { ->
        { request ->
            val userId = authLens(request)
            val friends = service.getFriends(userId).map { it.toDtoV1() }
            Response(Status.OK).with(Lenses.users of friends.toTypedArray())
        }
    }

    fun routes(): List<ContractRoute> = listOf(
        listGames,
        refreshGames,
        getLoginUrl,
        authCallback,
        getUser,
        listAchievements,
        refreshAchievements,
        updateGame,
        updateAchievement,
        getFriends,
        listAchievementsForFriend
    )
}