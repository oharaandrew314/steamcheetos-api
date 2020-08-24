package io.andrewohara.cheetosbros.api.auth.steam

import io.andrewohara.cheetosbros.sources.Platform
import io.andrewohara.cheetosbros.sources.Player
import io.andrewohara.cheetosbros.sources.Source
import org.openid4java.consumer.ConsumerManager
import org.openid4java.discovery.DiscoveryInformation
import org.openid4java.message.ParameterList
import java.util.regex.Pattern


class SteamOpenID(private val steamApi: Source) {
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
    fun verifyResponse(receivingUrl: String, responseMap: Map<String, String>): Player? {
        val responseList = ParameterList(responseMap)

        val verification = manager.verify(receivingUrl, responseList, discovered)
        val verifiedId = verification.verifiedId ?: return null

        val id = verifiedId.identifier
        val matcher = STEAM_REGEX.matcher(id)
        if (!matcher.find()) return null

        val steamId64 = matcher.group(1)
        val userProfile = steamApi.getPlayer(steamId64) ?: return null

        return Player(
                platform = Platform.Steam,
                id = steamId64,
                username = userProfile.username,
                avatar = userProfile.avatar
        )
    }
}