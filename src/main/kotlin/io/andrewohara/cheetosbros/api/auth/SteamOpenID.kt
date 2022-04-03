package io.andrewohara.cheetosbros.api.auth

import org.http4k.core.*
import org.openid4java.consumer.ConsumerManager
import org.openid4java.message.ParameterList
import java.util.regex.Pattern

class SteamOpenID {
    companion object {
        private const val STEAM_OPENID = "http://steamcommunity.com/openid"
        private val STEAM_REGEX = Pattern.compile("(\\d+)")
    }

    private val manager = ConsumerManager().apply {
        maxAssocAttempts = 0
    }

    private val discovered by lazy {
        manager.associate(manager.discover(STEAM_OPENID))
    }

    /**
     * Generate a steam login URL
     */
    fun getLoginUrl(callbackUri: Uri): Uri {
        val result = manager.authenticate(discovered, callbackUri.toString())
        return Uri.of(result.getDestinationUrl(true))
    }

    /**
     * Verify OpenID Authentication response and return steamId64
     */
    fun resolveUserId(receivingUri: Uri, params: Parameters): String? {
        val responseMap = ParameterList(params.toMap())

        val responseList = ParameterList(responseMap)

        val verification = manager.verify(receivingUri.toString(), responseList, discovered)
        val verifiedId = verification.verifiedId ?: return null

        val id = verifiedId.identifier
        val matcher = STEAM_REGEX.matcher(id)
        if (!matcher.find()) return null

        return matcher.group(1)
    }
}