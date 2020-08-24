package io.andrewohara.cheetosbros.api.games.v1

import io.andrewohara.cheetosbros.api.auth.CheetosRole
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.sources.Platform
import io.andrewohara.cheetosbros.sources.SyncManager
import io.javalin.Javalin
import io.javalin.core.security.SecurityUtil.roles
import io.javalin.core.validation.JavalinValidation
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse

class GamesControllerV1(private val gamesHandler: GamesManager, private val syncManager: SyncManager) {

    fun register(app: Javalin) {
        JavalinValidation.register(Platform::class.java, Platform::valueOf)

        app.post("/v1/sync", ::sync, roles(CheetosRole.User))
        app.get("/v1/games/:platform", ::listGames, roles(CheetosRole.User))
        app.get("/v1/games/:platform/:game_id", ::getGame, roles(CheetosRole.User))
        app.get("/v1/games/:platform/:game_id/achievements", ::listAchievements, roles(CheetosRole.User))
        app.get("/v1/games/:platform/:game_id/achievements/:player_id", ::listAchievementStatus, roles(CheetosRole.User))
    }

    private fun sync(ctx: Context) {
        val user = ctx.attribute<User>("user")!!

        syncManager.sync(user)
    }

    private fun listGames(ctx: Context) {
        val user = ctx.attribute<User>("user")!!
        val platform = ctx.pathParam<Platform>("platform").get()

        val games = gamesHandler.listGames(user, platform)

        ctx.json(games)
    }

    private fun getGame(ctx: Context) {
        val platform = ctx.pathParam<Platform>("platform").get()
        val gameId = ctx.pathParam("game_id")

        val game = gamesHandler.getGame(platform, gameId) ?: throw NotFoundResponse()

        ctx.json(game)
    }

    private fun listAchievements(ctx: Context) {
        val platform = ctx.pathParam<Platform>("platform").get()
        val gameId = ctx.pathParam("game_id")

        val achievements = gamesHandler.listAchievements(platform, gameId) ?: throw NotFoundResponse()

        ctx.json(achievements)
    }

    private fun listAchievementStatus(ctx: Context) {
        val user = ctx.attribute<User>("user")!!
        val gameId = ctx.pathParam("game_id")
        val playerId = ctx.pathParam("player_id")

        val achievements = gamesHandler.listAchievementStatus(user, playerId = playerId, gameId = gameId) ?: throw NotFoundResponse()

        ctx.json(achievements)
    }
}