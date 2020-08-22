package io.andrewohara.cheetosbros.api.users

import io.andrewohara.cheetosbros.api.auth.CheetosRole
import io.javalin.Javalin
import io.javalin.core.security.SecurityUtil.roles
import io.javalin.http.Context

class UsersControllerV1(app: Javalin, private val usersManager: UsersManager) {

    init {
        app.get("/v1/friends", ::getFriends, roles(CheetosRole.User))
    }

    private fun getFriends(ctx: Context) {
        val user = ctx.attribute<User>("user")!!

        val friends = usersManager.getFriends(user)

        ctx.json(friends)
    }
}