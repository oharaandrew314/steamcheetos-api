package io.andrewohara.cheetosbros.api

import io.andrewohara.cheetosbros.api.auth.openxbl.OpenXblAuthController
import io.andrewohara.cheetosbros.api.auth.steam.SteamAuthController
import io.andrewohara.cheetosbros.api.auth.xbox.XboxAuthController
import io.andrewohara.cheetosbros.api.games.v1.GamesControllerV1
import io.andrewohara.cheetosbros.api.users.InMemoryUsersManager
import io.andrewohara.cheetosbros.sources.openxbl.OpenXblSource
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
            val openXblApiKey = System.getenv("OPENXBL_API_KEY")
            val openXblPublicAppKey = System.getenv("OPENXBL_PUBLIC_APP_KEY")

            val app = Javalin.create {
                // TODO implement
                it.accessManager { handler, ctx, permittedRoles ->
                    handler.handle(ctx)
                }
            }

            val steamSource = SteamSource(steamApiKey)
            val xboxSource = OpenXblSource(openXblApiKey)
            val users = InMemoryUsersManager(steamSource)

            SteamAuthController(users).register(app)
//            XboxAuthController(xboxClientId, xboxClientSecret, users).register(app)
            OpenXblAuthController(openXblPublicAppKey, users).register(app)
            GamesControllerV1(steamSource = steamSource, xboxSource = xboxSource).register(app)

            app.start(8000)
        }
    }

    enum class ApiRole: Role {
        Public, User
    }
}