package io.andrewohara.cheetosbros.api

import io.andrewohara.cheetosbros.api.auth.CheetosAuthorizationHandler
import io.andrewohara.cheetosbros.api.auth.openxbl.OpenXblAuthController
import io.andrewohara.cheetosbros.api.auth.steam.SteamAuthController
import io.andrewohara.cheetosbros.api.games.v1.GamesControllerV1
import io.andrewohara.cheetosbros.api.games.v1.GamesHandler
import io.andrewohara.cheetosbros.api.users.InMemoryUsersManager
import io.andrewohara.cheetosbros.api.users.v1.UsersControllerV1
import io.andrewohara.cheetosbros.sources.openxbl.OpenXblSource
import io.andrewohara.cheetosbros.sources.steam.SteamSource
import io.javalin.Javalin

class ApiServer {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val steamApiKey = System.getenv("STEAM_API_KEY")
            val openXblApiKey = System.getenv("OPENXBL_API_KEY")
            val openXblPublicAppKey = System.getenv("OPENXBL_PUBLIC_APP_KEY")

            val steamSource = SteamSource(steamApiKey)
            val xboxSource = OpenXblSource(openXblApiKey)
            val users = InMemoryUsersManager(steamSource)

            val gamesHandler = GamesHandler(xboxSource = xboxSource, steamSource = steamSource)

            val app = Javalin.create {
                it.accessManager(CheetosAuthorizationHandler(users))
                it.enableCorsForAllOrigins()
            }

            SteamAuthController(users).register(app)
            OpenXblAuthController(openXblPublicAppKey, users).register(app)
            GamesControllerV1(gamesHandler).register(app)
            UsersControllerV1().register(app)

            app.start(8000)
        }
    }
}