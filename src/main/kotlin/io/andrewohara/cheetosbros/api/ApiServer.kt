package io.andrewohara.cheetosbros.api

import io.andrewohara.cheetosbros.api.auth.AuthManager
import io.andrewohara.cheetosbros.api.auth.JwtAuthorizationDao
import io.andrewohara.cheetosbros.api.auth.PemUtils
import io.andrewohara.cheetosbros.api.auth.openxbl.OpenXblAuthController
import io.andrewohara.cheetosbros.api.auth.steam.SteamAuthController
import io.andrewohara.cheetosbros.api.games.v1.GameStatusDao
import io.andrewohara.cheetosbros.api.games.v1.GamesControllerV1
import io.andrewohara.cheetosbros.api.games.v1.GamesDao
import io.andrewohara.cheetosbros.api.games.v1.GamesHandler
import io.andrewohara.cheetosbros.api.users.UsersDao
import io.andrewohara.cheetosbros.api.users.UsersManager
import io.andrewohara.cheetosbros.sources.steam.SteamSource
import io.javalin.Javalin
import java.nio.file.Paths

class ApiServer {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val steamApiKey = System.getenv("STEAM_API_KEY")
            val openXblPublicAppKey = System.getenv("OPENXBL_PUBLIC_APP_KEY")

            val steamSource = SteamSource(steamApiKey)

            val gamesDao = GamesDao("cheetosbros-games-dev")

            val gameStatusDao = GameStatusDao("cheetosbros-gamestatus-dev")

            val usersDao = UsersDao("cheetosbros-users-dev")

            val authorizationDao = JwtAuthorizationDao(
                    issuer = "cheetosbros-dev",
                    privateKey = PemUtils.parsePEMFile(Paths.get("C:/Users/ohara/Desktop/cheetosbros-dev.pem").toUri().toURL())!!,
                    publicKey = PemUtils.parsePEMFile(Paths.get("C:/Users/ohara/Desktop/cheetosbros-dev-pub.pem").toUri().toURL())!!
            )

            val usersManager = UsersManager(usersDao)

            val gamesHandler = GamesHandler(
                    steamSource = steamSource,
                    gamesDao = gamesDao,
                    gameStatusDao = gameStatusDao
            )

            val authorizationHandler = AuthManager(authorizationDao, usersManager)

            val app = Javalin.create {
                it.accessManager(authorizationHandler)
                it.enableCorsForAllOrigins()
            }

            app.get("/health") { it.result("ok") }

            SteamAuthController(steamSource, authorizationHandler).register(app)
            OpenXblAuthController(openXblPublicAppKey, authorizationHandler).register(app)
            GamesControllerV1(gamesHandler).register(app)

            app.start(8000)
        }
    }
}