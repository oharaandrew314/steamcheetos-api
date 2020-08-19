package io.andrewohara.cheetosbros.api.games.v1

import io.andrewohara.cheetosbros.api.auth.CheetosRole
import io.andrewohara.cheetosbros.api.users.User
import io.javalin.Javalin
import io.javalin.core.security.SecurityUtil.roles
import io.javalin.http.Context

class GamesControllerV1(private val gamesHandler: GamesHandler) {

    fun register(app: Javalin) {
        app.post("/v1/games/sync", ::sync, roles(CheetosRole.User))
        app.get("/v1/games", ::listGames, roles(CheetosRole.User))
    }

    private fun sync(ctx: Context) {
        val user = ctx.attribute<User>("user")!!

        gamesHandler.sync(user)
    }

    private fun listGames(ctx: Context) {
        val user = ctx.attribute<User>("user")!!

        val games = gamesHandler.listGames(user)

        ctx.json(games)
    }
}