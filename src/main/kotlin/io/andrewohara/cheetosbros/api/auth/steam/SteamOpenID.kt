package io.andrewohara.cheetosbros.api.auth.steam

import org.openid4java.consumer.ConsumerManager
import org.openid4java.discovery.DiscoveryInformation
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

    private var discovered: DiscoveryInformation = manager.associate(manager.discover(STEAM_OPENID))

    /**
     * Generate a steam login URL
     */
    fun getLoginUrl(callbackUrl: String): String {
        val authReq = manager.authenticate(discovered, callbackUrl)
        return authReq.getDestinationUrl(true)
    }

    /**
     * Verify OpenID Authentication response and return steamId64
     */
    fun verifyResponse(receivingUrl: String, responseMap: Map<String, String>): String? {
        val responseList = ParameterList(responseMap)

        val verification = manager.verify(receivingUrl, responseList, discovered)
        val verifiedId = verification.verifiedId ?: return null

        val id = verifiedId.identifier
        val matcher = STEAM_REGEX.matcher(id)
        if (!matcher.find()) return null

        return matcher.group(1)
    }
}