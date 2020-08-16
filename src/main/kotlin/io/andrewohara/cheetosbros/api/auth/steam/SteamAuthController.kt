package io.andrewohara.cheetosbros.api.auth.steam

import io.andrewohara.cheetosbros.api.auth.CheetosRole
import io.andrewohara.cheetosbros.api.users.UsersManager
import io.javalin.Javalin
import io.javalin.core.security.SecurityUtil.roles
import io.javalin.http.Context
import io.javalin.http.UnauthorizedResponse
import org.apache.http.client.utils.URIBuilder

class SteamAuthController(private val users: UsersManager) {

    private val steamOpenId = SteamOpenID()

    private val frontendRedirectUrl = "http://localhost:3000/auth/callback"

    fun register(app: Javalin) {
        app.get("/v1/auth/steam/login", ::login, roles(CheetosRole.Public))

        app.get("/v1/auth/steam/callback", ::callback, roles(CheetosRole.Public))
    }

    private fun login(ctx: Context) {
        val steamRedirectUrl = URIBuilder().apply {
            scheme = ctx.scheme()
            host = ctx.host()
            path = "/v1/auth/steam/callback"
        }.build().toString()

        val loginUrl = steamOpenId.getLoginUrl(steamRedirectUrl)
        ctx.redirect(loginUrl)
    }

    private fun callback(ctx: Context) {
        val params = ctx.queryParamMap().filterKeys { it.startsWith("openid") }.mapValues { it.value.first() }
        val steamId64 = steamOpenId.verifyResponse(ctx.url(), params)
                ?: throw UnauthorizedResponse()

        val user = users.getUserBySteamId64(steamId64) ?: users.createUser(steamId64)
        val token = users.assignToken(user.id)

        ctx.redirect("$frontendRedirectUrl?token=$token")
    }
}