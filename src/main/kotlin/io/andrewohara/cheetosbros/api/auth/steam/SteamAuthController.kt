package io.andrewohara.cheetosbros.api.auth.steam

import io.andrewohara.cheetosbros.api.auth.CheetosAuthorizationHandler
import io.andrewohara.cheetosbros.api.auth.CheetosRole
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.api.users.UsersManager
import io.javalin.Javalin
import io.javalin.core.security.SecurityUtil.roles
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.UnauthorizedResponse
import org.apache.http.client.utils.URIBuilder
import java.lang.IllegalStateException

class SteamAuthController(private val users: UsersManager, private val cheetosAuth: CheetosAuthorizationHandler) {

    private val steamOpenId = SteamOpenID()

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
        val steamId64 = steamOpenId.verifyResponse(ctx.url(), params)
                ?: throw UnauthorizedResponse()

        val loggedInUser = ctx.attribute<User>("user")
        val linkedUser = users.getUserBySteamId64(steamId64)

        when {
            linkedUser == null && loggedInUser == null -> {
                val newUser = users.createUser()
                users.linkSocialLogin(newUser.id, steamId64)

                users.linkSession(newUser.id, cheetosAuth.createSession(ctx))
            }
            linkedUser == null && loggedInUser != null -> {
                users.linkSocialLogin(loggedInUser.id, steamId64)
            }
            linkedUser != null && loggedInUser == null -> {
                users.linkSession(linkedUser.id, cheetosAuth.createSession(ctx))
            }
            linkedUser != null && loggedInUser != null -> {
                if (linkedUser.id != loggedInUser.id) throw BadRequestResponse("User is already linked to another account")
            }
            else -> throw IllegalStateException()
        }

        ctx.redirect(frontendRedirectUrl)
    }
}