package io.andrewohara.cheetosbros.api.auth.steam

import io.andrewohara.cheetosbros.api.auth.AuthManager
import io.andrewohara.cheetosbros.api.auth.CheetosRole
import io.andrewohara.cheetosbros.sources.steam.SteamSource
import io.javalin.Javalin
import io.javalin.core.security.SecurityUtil.roles
import io.javalin.http.Context
import io.javalin.http.UnauthorizedResponse
import org.apache.http.client.utils.URIBuilder

class SteamAuthController(private val steamApi: SteamSource, private val authManager: AuthManager) {

    private val steamOpenId = SteamOpenID(steamApi)

    private val frontendRedirectUrl = "http://localhost:3000/auth/callback"

    fun register(app: Javalin) {
        app.get("/v1/auth/steam/login", ::login, roles(CheetosRole.Public, CheetosRole.User))
        app.get("/v1/auth/steam/callback", ::callback, roles(CheetosRole.Public, CheetosRole.User))
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
        val socialLink = steamOpenId.verifyResponse(ctx.url(), params) ?: throw UnauthorizedResponse()

        authManager.login(ctx, socialLink)

        ctx.redirect(frontendRedirectUrl)
    }
}