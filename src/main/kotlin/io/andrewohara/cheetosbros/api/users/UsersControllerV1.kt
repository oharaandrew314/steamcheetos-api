package io.andrewohara.cheetosbros.api.users

import io.andrewohara.cheetosbros.api.auth.CheetosRole
import io.andrewohara.cheetosbros.sources.Platform
import io.javalin.Javalin
import io.javalin.core.security.SecurityUtil.roles
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse

class UsersControllerV1(app: Javalin, private val usersManager: UsersManager) {

    init {
        app.get("/v1/players/me/:platform", ::getMyPlayer, roles(CheetosRole.User))
    }

    private fun getMyPlayer(ctx: Context) {
        val user = ctx.attribute<User>("user")!!
        val platform = ctx.pathParam<Platform>("platform").get()

        val player = usersManager.getPlayer(user, platform) ?: throw NotFoundResponse()

        ctx.json(player)
    }
}