package io.andrewohara.cheetosbros.api.v1

import io.andrewohara.cheetosbros.api.auth.AuthManager
import io.andrewohara.cheetosbros.api.auth.SteamOpenID
import io.andrewohara.cheetosbros.api.users.User
import org.apache.http.client.utils.URIBuilder
import spark.Request
import spark.Response
import spark.Spark.*
import java.net.URLDecoder

class AuthApiV1(
    private val authManager: AuthManager,
    private val steamOpenId: SteamOpenID,
    private val frontendHost: String,
    private val decodeQueryParams: Boolean
    ) {

    init {
        before(authManager)

        // steam
        get("/v1/auth/steam/login", ::loginSteam)
        get("/v1/auth/steam/callback", ::callbackSteam)
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
            "openid.return_to" to request.param("openid.return_to"),
            "openid.identity" to request.param("openid.identity"),
            "openid.op_endpoint" to request.param("openid.op_endpoint"),
            "openid.assoc_handle" to request.param("openid.assoc_handle"),
            "openid.mode" to request.param("openid.mode"),
            "openid.signed" to request.param("openid.signed"),
            "openid.sig" to request.param("openid.sig"),
            "openid.claimed_id" to request.param("openid.claimed_id"),
            "openid.response_nonce" to request.param("openid.response_nonce"),
            "openid.ns" to request.param("openid.ns")
        )

        val player = steamOpenId.verifyResponse(request.url(), params) ?: throw halt(401)

        val sessionToken = authManager.assignSessionToken(user, player)
        response.redirect("$frontendHost/auth/callback?session=$sessionToken")
    }

    private fun Request.param(param: String) = if (decodeQueryParams) {
        URLDecoder.decode(queryParams(param), Charsets.UTF_8)
    } else {
        queryParams(param)
    }
}