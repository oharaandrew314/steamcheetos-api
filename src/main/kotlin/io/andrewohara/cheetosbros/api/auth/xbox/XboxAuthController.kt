package io.andrewohara.cheetosbros.api.auth.xbox

import io.andrewohara.cheetosbros.api.users.UsersManager
import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.UnauthorizedResponse
import org.apache.http.client.utils.URIBuilder
import java.net.URI

class XboxAuthController(clientId: String, clientSecret: String, private val users: UsersManager) {

    private val authHelper = XboxOpenIdConnect(clientId, clientSecret)
    private val frontendRedirectUrl = "http://localhost:3000/auth/steam/callback"

    fun register(app: Javalin) {
        app.get("/v1/auth/xbox/login", ::login)
        app.get("/v1/auth/xbox/callback", ::callback)
    }

    private fun login(ctx: Context) {
        val cheetosCallback = URIBuilder().apply {
            scheme = ctx.scheme()
            host = ctx.host()
            path = "/v1/auth/xbox/callback"
        }.build()

        val authUrl = authHelper.getAuthRedirectUrl(cheetosCallback)
        ctx.redirect(authUrl)
    }

    private fun callback(ctx: Context) {
        println("callback")
        println(ctx.queryParamMap())

        val authentication = authHelper.authorizeCallback(ctx.queryParamMap(), URI(ctx.url()), URI(ctx.fullUrl())) ?: throw UnauthorizedResponse()
        val username = authentication.account().username()

        val user = users.getUserByXboxUsername(username) ?: users.createUser(xboxUsername = username)
        val token = users.assignToken(user.id)

        ctx.redirect("$frontendRedirectUrl?token=$token")
    }
}