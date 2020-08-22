package io.andrewohara.cheetosbros.api.games.v1

import io.andrewohara.cheetosbros.api.auth.CheetosRole
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.sources.SyncManager
import io.javalin.Javalin
import io.javalin.core.security.SecurityUtil.roles
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse

class GamesControllerV1(private val gamesHandler: GamesManager, private val syncManager: SyncManager) {

    fun register(app: Javalin) {
        app.post("/v1/games/sync", ::sync, roles(CheetosRole.User))
        app.get("/v1/games", ::listGames, roles(CheetosRole.User))
        app.get("/v1/games/:uuid", ::getGame, roles(CheetosRole.User))
        app.get("/v1/games/:uuid/achievements", ::listAchievements, roles(CheetosRole.User))
    }

    private fun sync(ctx: Context) {
        val user = ctx.attribute<User>("user")!!

        syncManager.sync(user)
    }

    private fun listGames(ctx: Context) {
        val user = ctx.attribute<User>("user")!!

        val games = gamesHandler.listGames(user)

        ctx.json(games)
    }

    private fun getGame(ctx: Context) {
        val gameUuid = ctx.pathParam("uuid")

        val game = gamesHandler.getGame(gameUuid) ?: throw NotFoundResponse()

        ctx.json(game)
    }

    private fun listAchievements(ctx: Context) {
        val user = ctx.attribute<User>("user")!!
        val gameUuid = ctx.pathParam("uuid")

        val achievements = gamesHandler.listAchievements(user, gameUuid) ?: throw NotFoundResponse()

        ctx.json(achievements)
    }
}