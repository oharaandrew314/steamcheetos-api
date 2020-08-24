package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.api.users.User

class FakeSourceFactory(private val steamSource: FakeSource, private val xboxSource: FakeSource): SourceFactory {

    override fun get(user: User, platform: Platform) = when(platform) {
        Platform.Xbox -> xboxSource
        Platform.Steam -> steamSource
    }
}