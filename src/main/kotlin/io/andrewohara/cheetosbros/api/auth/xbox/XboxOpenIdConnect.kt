package io.andrewohara.cheetosbros.api.auth.xbox


import com.microsoft.aad.msal4j.*
import com.nimbusds.jwt.JWTParser
import com.nimbusds.oauth2.sdk.AuthorizationCode
import com.nimbusds.openid.connect.sdk.AuthenticationErrorResponse
import com.nimbusds.openid.connect.sdk.AuthenticationResponse
import com.nimbusds.openid.connect.sdk.AuthenticationResponseParser
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.util.*

// TODO implement logout https://docs.microsoft.com/en-us/azure/active-directory/develop/scenario-web-app-sign-user-sign-in?tabs=java#sign-out-button

class XboxOpenIdConnect(clientId: String, clientSecret: String){
    companion object {
        private const val authority = "https://login.microsoftonline.com/common/"
        private val scopes = setOf("xboxlive.signin", "profile")
        private fun AuthenticationResponse.isSuccess() = this is AuthenticationSuccessResponse
        private fun IAuthenticationResult.getNonce(): String? = JWTParser.parse(idToken()).jwtClaimsSet.getClaim("nonce") as? String
    }

    private val publicApplication = PublicClientApplication.builder(clientId).build()
    private val privateApplication = ConfidentialClientApplication
            .builder(clientId, ClientCredentialFactory.createFromSecret(clientSecret))
            .authority(authority)
            .build()

    fun authorizeCallback(params: Map<String, List<String>>, currentUri: URI, fullUrl: URI): IAuthenticationResult? {
        val authResponse = AuthenticationResponseParser.parse(fullUrl, params)

        if (!authResponse.isSuccess()) {
            val oidcResponse = authResponse as AuthenticationErrorResponse
            print(oidcResponse.errorObject)
            return null
        }

        val oidcResponse = authResponse as AuthenticationSuccessResponse
        if (oidcResponse.authorizationCode == null) return null

        val result = getAuthResultByAuthCode(oidcResponse.authorizationCode, currentUri)
        if (result.getNonce().isNullOrBlank()) return null

        println(result.scopes())

        println(JWTParser.parse(result.idToken()).jwtClaimsSet)

        return result
    }

    fun getAuthRedirectUrl(redirectURL: URI): String {
        val state = UUID.randomUUID().toString()
        val nonce = UUID.randomUUID().toString()
        return getAuthorizationCodeUrl(redirectURL, state, nonce)
    }

    private fun getAuthorizationCodeUrl(registeredRedirectURL: URI, state: String?, nonce: String?): String {
        val parameters = AuthorizationRequestUrlParameters
                .builder(registeredRedirectURL.toString(), scopes.map { URLEncoder.encode(it, "UTF-8") }.toSet())
                .responseMode(ResponseMode.QUERY)
                .prompt(Prompt.SELECT_ACCOUNT)
                .state(state)
                .nonce(nonce)
//                .claimsChallenge("user")
                .build()

        return publicApplication.getAuthorizationRequestUrl(parameters).toString()
    }

    private fun getAuthResultByAuthCode(authorizationCode: AuthorizationCode, currentUri: URI): IAuthenticationResult {
        val parameters = AuthorizationCodeParameters
                .builder(authorizationCode.value, currentUri)
                .build()

        return privateApplication.acquireToken(parameters).get() ?: throw IOException("could not get token")
    }
}
