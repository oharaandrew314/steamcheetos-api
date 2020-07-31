package io.andrewohara.cheetosbros.api

import io.andrewohara.cheetosbros.api.auth.steam.SteamAuthController
import io.javalin.Javalin

class ApiServer {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val app = Javalin.create()
            SteamAuthController().register(app)

            app.start(8000)
        }
    }
}