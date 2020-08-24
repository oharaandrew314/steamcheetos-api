package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.sources.openxbl.OpenXblSource
import io.andrewohara.cheetosbros.sources.steam.SteamSource

interface SourceFactory {
    operator fun get(user: User, platform: Platform): Source?
}

class SourceFactoryImpl(steamKey: String): SourceFactory {

    private val steamSource = SteamSource(steamKey)

    override fun get(user: User, platform: Platform) = when(platform) {
        Platform.Xbox -> user.openxblToken?.let { OpenXblSource(it) }
        Platform.Steam -> steamSource
    }
}