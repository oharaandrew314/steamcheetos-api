package io.andrewohara.cheetosbros.api.auth.steam

import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.UnauthorizedResponse
import org.eclipse.jetty.http.HttpStatus

class SteamAuthController {

    private val steamOpenId = SteamOpenID()

    fun register(app: Javalin) {
        app.get("/auth/steam/login", ::login)

        app.get("/auth/steam/callback", ::callback)
    }

    private fun login(ctx: Context) {
        val url = steamOpenId.getLoginUrl("${ctx.scheme()}://${ctx.host()}/auth/steam/callback")
        ctx.redirect(url, HttpStatus.MOVED_PERMANENTLY_301)
    }

    private fun callback(ctx: Context) {
        val steamId64 = steamOpenId.verifyResponse(ctx.url(), ctx.queryParamMap().mapValues { it.value.first() })
                ?: throw UnauthorizedResponse()

        ctx.result("You are steam user $steamId64")
    }
}