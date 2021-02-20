package io.andrewohara.cheetosbros.api.auth.openxbl

import io.andrewohara.cheetosbros.api.auth.AuthManager
import io.andrewohara.cheetosbros.api.auth.CheetosRole
import io.javalin.Javalin
import io.javalin.core.security.SecurityUtil
import io.javalin.http.Context

class OpenXblAuthController(publicAppKey: String, private val authManager: AuthManager) {

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
        val player = openXblAuth.verify(code)

        authManager.login(ctx, player)

        ctx.redirect(frontendRedirectUrl)
    }
}