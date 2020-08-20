package io.andrewohara.cheetosbros.api.users

import io.andrewohara.cheetosbros.sources.Game

data class User(
        val id: String,
        val displayName: String,
        val xbox: SocialLink? = null,
        val steam: SocialLink? = null
) {
    fun socialLinkForPlatform(platform: Game.Platform) = when(platform) {
        Game.Platform.Xbox -> xbox
        Game.Platform.Steam -> steam
    }

    fun withSocialLink(socialLink: SocialLink): User {
        return when(socialLink.platform) {
            Game.Platform.Xbox -> copy(xbox = socialLink)
            Game.Platform.Steam -> copy(steam = socialLink)
        }
    }
}

data class SocialLink(
        val id: String,
        val username: String,
        val platform: Game.Platform,
        val token: String?
)