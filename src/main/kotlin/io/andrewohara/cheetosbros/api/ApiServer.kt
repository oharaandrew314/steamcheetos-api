package io.andrewohara.cheetosbros.api

import io.andrewohara.cheetosbros.api.auth.steam.SteamAuthController
import io.andrewohara.cheetosbros.api.auth.xbox.XboxAuthController
import io.andrewohara.cheetosbros.api.users.InMemoryUsersManager
import io.andrewohara.cheetosbros.sources.steam.SteamSource
import io.javalin.Javalin
import io.javalin.core.security.Role

class ApiServer {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val xboxClientId = System.getenv("XBOX_CLIENT_ID")
            val xboxClientSecret = System.getenv("XBOX_CLIENT_SECRET")
            val steamApiKey = System.getenv("STEAM_API_KEY")

            val app = Javalin.create()

            val steamSource = SteamSource(steamApiKey)
            val users = InMemoryUsersManager(steamSource)

            SteamAuthController(users).register(app)
            XboxAuthController(xboxClientId, xboxClientSecret, users).register(app)

            app.start(8000)
        }
    }

    enum class ApiRole: Role {
        Public, User
    }
}