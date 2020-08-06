package io.andrewohara.cheetosbros.api

import io.andrewohara.cheetosbros.api.auth.steam.SteamAuthController
import io.andrewohara.cheetosbros.api.auth.xbox.XboxAuthController
import io.andrewohara.cheetosbros.api.users.InMemoryUsersManager
import io.javalin.Javalin
import io.javalin.core.security.Role

class ApiServer {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val xboxClientId = System.getenv("XBOX_CLIENT_ID")
            val xboxClientSecret = System.getenv("XBOX_CLIENT_SECRET")

            val app = Javalin.create()

            val users = InMemoryUsersManager()

            SteamAuthController(users).register(app)
            XboxAuthController(xboxClientId, xboxClientSecret, users).register(app)

            app.start(8000)
        }
    }

    enum class ApiRole: Role {
        Public, User
    }
}