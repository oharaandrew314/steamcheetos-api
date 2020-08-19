package io.andrewohara.cheetosbros.api.auth.openxbl

import io.andrewohara.cheetosbros.api.auth.CheetosAuthorizationHandler
import io.andrewohara.cheetosbros.api.auth.CheetosRole
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.api.users.UsersManager
import io.javalin.Javalin
import io.javalin.core.security.SecurityUtil
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import java.lang.IllegalStateException

class OpenXblAuthController(publicAppKey: String, private val users: UsersManager, private val cheetosAuth: CheetosAuthorizationHandler) {

    private val openXblAuth = OpenXblAuth(publicAppKey)

    private val frontendRedirectUrl = "http://localhost:3000/auth/callback"

    fun register(app: Javalin) {
        app.get("/v1/auth/openxbl/login", ::login, SecurityUtil.roles(CheetosRole.Public, CheetosRole.User))
        app.get("/v1/auth/openxbl/callback", ::callback, SecurityUtil.roles(CheetosRole.Public, CheetosRole.User))
    }

    private fun login(ctx: Context) {
        val loginUrl = openXblAuth.getLoginUrl()
        ctx.redirect(loginUrl)
    }

    private fun callback(ctx: Context) {
        val code = ctx.queryParam<String>("code").get()
        val result = openXblAuth.verify(code)

        val loggedInUser = ctx.attribute<User>("user")
        val linkedUser = users.getUserByXuid(result.xuid)

        when {
            linkedUser == null && loggedInUser == null -> {
                val newUser = users.createUser()
                users.linkSocialLogin(newUser.id, result)

                users.linkSession(newUser.id, cheetosAuth.createSession(ctx))
            }
            linkedUser == null && loggedInUser != null -> {
                users.linkSocialLogin(loggedInUser.id, result)
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