package io.andrewohara.cheetosbros.api

import io.andrewohara.cheetosbros.api.auth.AuthManager
import io.andrewohara.cheetosbros.api.auth.steam.SteamOpenID
import io.andrewohara.cheetosbros.api.games.v1.GamesManager
import io.andrewohara.cheetosbros.api.games.v1.OwnedGame
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.sources.Achievement
import io.andrewohara.cheetosbros.sources.AchievementStatus
import io.andrewohara.cheetosbros.sources.Platform
import io.andrewohara.cheetosbros.sources.SourceManager
import org.apache.http.client.utils.URIBuilder
import spark.Request
import spark.Response
import spark.Spark.*
import java.lang.IllegalArgumentException
import java.net.URLDecoder

class SparkApi(
    private val gamesManager: GamesManager,
    private val authManager: AuthManager,
    private val steamOpenId: SteamOpenID,
    private val sourceManger: SourceManager,
    private val frontendHost: String
    ) {

    init {
        before(authManager)

        //health
        get("/health") { _, _ -> "OK" }

        // games
        get("/v1/games", ::listGames)
        get("/v1/games/:platform/:game_id", ::getGame)
        get("/v1/games/:platform/:game_id/achievements", ::listAchievements)
        get("/v1/games/:platform/:game_id/achievements/status", ::listAchievementStatus)

        // steam
        get("/v1/auth/steam/login", ::loginSteam)
        get("/v1/auth/steam/callback", ::callbackSteam)

        // sync
        post("/v1/sync", ::sync)
    }

    private fun listGames(request: Request, response: Response): Collection<OwnedGame> {
        val user = request.attribute<User>("user") ?: throw halt(401)

        return gamesManager.listGames(user)
    }

    private fun getGame(request: Request, response: Response): OwnedGame {
        val user = request.attribute<User>("user") ?: throw halt(401)
        val platform = request.params("platform").toPlatform()
        val gameId = request.params("game_id")

        val player = user.players[platform]
            ?: throw halt(404, "User does not have a $platform player")

        return gamesManager.getGame(player, gameId)
            ?: throw halt(404, "Could not find game $gameId")
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

    private fun loginSteam(request: Request, response: Response) {
        val steamRedirectUrl = URIBuilder().apply {
            scheme = request.scheme()
            host = request.host()
            path = "/v1/auth/steam/callback"
        }.build().toString()

        val loginUrl = steamOpenId.getLoginUrl(steamRedirectUrl)

        response.redirect(loginUrl)
    }

    private fun callbackSteam(request: Request, response: Response) {
        val user = request.attribute<User>("user")

        val params = mapOf(
            "openid.return_to" to request.decodeQueryParam("openid.return_to"),
            "openid.identity" to request.decodeQueryParam("openid.identity"),
            "openid.op_endpoint" to request.decodeQueryParam("openid.op_endpoint"),
            "openid.assoc_handle" to request.decodeQueryParam("openid.assoc_handle"),
            "openid.mode" to request.decodeQueryParam("openid.mode"),
            "openid.signed" to request.decodeQueryParam("openid.signed"),
            "openid.sig" to request.decodeQueryParam("openid.sig"),
            "openid.claimed_id" to request.decodeQueryParam("openid.claimed_id"),
            "openid.response_nonce" to request.decodeQueryParam("openid.response_nonce"),
            "openid.ns" to request.decodeQueryParam("openid.ns")
        )

        println("foo")
        println(params)

        val player = steamOpenId.verifyResponse(request.url(), params) ?: throw halt(401)

        val sessionToken = authManager.assignSessionToken(user, player)
        response.redirect("$frontendHost/auth/callback?session=$sessionToken")
    }

    private fun sync(request: Request, response: Response) {
        val user = request.attribute<User>("user") ?: throw halt(401)

        for (player in user.players.values) {
            sourceManger.sync(player)
        }
    }

    private fun String.toPlatform() = try {
        Platform.valueOf(this)
    } catch (e: IllegalArgumentException) {
        throw halt(404, "Invalid platform: $this")
    }

    private fun Request.decodeQueryParam(param: String) = URLDecoder.decode(queryParams(param), Charsets.UTF_8)
}