package io.andrewohara.cheetosbros.api.auth.openxbl

import io.andrewohara.cheetosbros.api.auth.CheetosRole
import io.andrewohara.cheetosbros.api.users.UsersManager
import io.javalin.Javalin
import io.javalin.core.security.SecurityUtil
import io.javalin.http.Context

class OpenXblAuthController(publicAppKey: String, private val users: UsersManager) {

    private val auth = OpenXblAuth(publicAppKey)

    private val frontendRedirectUrl = "http://localhost:3000/auth/callback"

    fun register(app: Javalin) {
        app.get("/v1/auth/openxbl/login", ::login, SecurityUtil.roles(CheetosRole.Public))
        app.get("/v1/auth/openxbl/callback", ::callback, SecurityUtil.roles(CheetosRole.Public))
    }

    private fun login(ctx: Context) {
        val loginUrl = auth.getLoginUrl()
        ctx.redirect(loginUrl)
    }

    private fun callback(ctx: Context) {
        val code = ctx.queryParam<String>("code").get()
        val result = auth.verify(code)

        val user = users.getUserByXuid(result.xuid) ?: users.createUser(xuid = result.xuid, gamertag = result.gamertag, openXblToken = result.app_key)
        val token = users.assignToken(user.id)

        ctx.redirect("$frontendRedirectUrl?token=$token")
    }
}