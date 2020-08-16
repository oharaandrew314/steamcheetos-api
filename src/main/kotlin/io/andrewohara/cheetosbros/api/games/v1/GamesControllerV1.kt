package io.andrewohara.cheetosbros.api.games.v1

import io.andrewohara.cheetosbros.api.ApiServer
import io.andrewohara.cheetosbros.sources.Source
import io.javalin.Javalin
import io.javalin.core.security.SecurityUtil.roles
import io.javalin.http.Context

class GamesControllerV1(private val steamSource: Source, private val xboxSource: Source) {

    fun register(app: Javalin) {
        app.get("/v1/games", ::listGames, roles(ApiServer.ApiRole.User))
    }

    private fun listGames(ctx: Context) {
        ctx.json(emptyList<String>())
    }
}