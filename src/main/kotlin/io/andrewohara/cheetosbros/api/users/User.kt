package io.andrewohara.cheetosbros.api.users

import io.andrewohara.cheetosbros.sources.Game

data class User(
        val id: String,
        val displayName: String? = null,
        val xbox: SocialLink? = null,
        val steam: SocialLink? = null
) {
    fun socialLinkForPlatform(platform: Game.Platform) = when(platform) {
        Game.Platform.Xbox -> xbox
        Game.Platform.Steam -> steam
    }
}

data class SocialLink(
        val id: String,
        val username: String,
        val platform: Game.Platform,
        val token: String?
)