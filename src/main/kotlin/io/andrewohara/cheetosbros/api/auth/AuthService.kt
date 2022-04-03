package io.andrewohara.cheetosbros.api.auth

import io.andrewohara.cheetosbros.api.v1.ApiV1
import org.http4k.core.*

class AuthService(
    private val steamOpenId: SteamOpenID,
    private val serverHost: Uri,
    private val authDao: AuthorizationDao
) {
    fun authorize(token: String): String? {
        return authDao.resolveUserId(token)
    }

    fun getLoginUri(clientRedirectUri: Uri): Uri {
        val severRedirect = Request(Method.GET, serverHost.path("/v1/auth/callback/${ApiV1.Lenses.clientCallback}"))
            .with(ApiV1.Lenses.clientCallback of clientRedirectUri.toString())

        return steamOpenId.getLoginUrl(severRedirect.uri)
    }

    fun callback(request: Request): Uri? {
        val clientCallback = ApiV1.Lenses.clientCallback(request)
        val userId = steamOpenId.resolveUserId(
            receivingUri = serverHost.extend(request.uri),
            params = request.uri.queries()
        ) ?: return null

        return Uri.of(clientCallback).query("token", authDao.assignToken(userId))
    }

//    fun getAccessToken(request: Request): String? {
//        val userId = steamOpenId.resolveUserId(
//            receivingUri = serverHost.extend(request.uri),
//            params = request.uri.queries()
//        ) ?: return null
//
//        return authDao.assignToken(userId)
//    }
}