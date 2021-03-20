package io.andrewohara.cheetosbros.api.v1

import io.andrewohara.cheetosbros.api.auth.AuthManager
import io.andrewohara.cheetosbros.api.auth.SteamOpenID
import io.andrewohara.cheetosbros.api.users.User
import org.apache.http.client.utils.URIBuilder
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status.Companion.FOUND
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Response
import org.http4k.lens.RequestContextLens
import org.http4k.routing.bind
import org.http4k.routing.routes

class AuthApiV1(
    private val authManager: AuthManager,
    private val steamOpenId: SteamOpenID,
    private val frontendHost: String,
    private val userLens: RequestContextLens<User?>,
//    private val decodeQueryParams: Boolean
    ) {

    fun getRoutes() = routes(
        "/v1/auth/steam/login" bind Method.GET to ::loginSteam,
        "/v1/auth/steam/callback" bind Method.GET to ::callbackSteam
    )

    private fun loginSteam(request: Request): Response {

        val steamRedirectUrl = URIBuilder(request.uri.toString()).apply {
            path = "/v1/auth/steam/callback"
        }.build().toString()

        val loginUrl = steamOpenId.getLoginUrl(steamRedirectUrl)

        return Response(FOUND).header("Location", loginUrl)
    }

    private fun callbackSteam(request: Request): Response {
        val user = userLens(request)

        val params = mapOf(
            "openid.return_to" to request.query("openid.return_to")!!,
            "openid.identity" to request.query("openid.identity")!!,
            "openid.op_endpoint" to request.query("openid.op_endpoint")!!,
            "openid.assoc_handle" to request.query("openid.assoc_handle")!!,
            "openid.mode" to request.query("openid.mode")!!,
            "openid.signed" to request.query("openid.signed")!!,
            "openid.sig" to request.query("openid.sig")!!,
            "openid.claimed_id" to request.query("openid.claimed_id")!!,
            "openid.response_nonce" to request.query("openid.response_nonce")!!,
            "openid.ns" to request.query("openid.ns")!!
        )

        val player = steamOpenId.verifyResponse(request.uri.toString(), params)
            ?: return Response(UNAUTHORIZED)

        val sessionToken = authManager.assignSessionToken(user, player)
            ?: return Response(FORBIDDEN).body("User is already linked to another account")

        return Response(FOUND).header("Location", "$frontendHost/auth/callback?session=$sessionToken")
    }

//    private fun Request.param(param: String) = if (decodeQueryParams) {
//        URLDecoder.decode(queryParams(param), Charsets.UTF_8)
//    } else {
//        queryParams(param)
//    }
}