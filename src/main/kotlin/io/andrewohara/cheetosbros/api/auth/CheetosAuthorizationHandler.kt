package io.andrewohara.cheetosbros.api.auth

import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.api.users.UsersManager
import io.javalin.core.security.AccessManager
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.UnauthorizedResponse

class CheetosAuthorizationHandler(private val users: UsersManager): AccessManager {

    override fun manage(handler: Handler, ctx: Context, permittedRoles: MutableSet<Role>) {
        val user = user(ctx)

        val currentRoles = if (user == null) {
            mutableSetOf(CheetosRole.Public)
        } else {
            ctx.attribute("user", user)
            mutableSetOf(CheetosRole.User)
        }

        if (currentRoles.none { it in permittedRoles }) throw UnauthorizedResponse()

        handler.handle(ctx)
    }

    private fun user(ctx: Context): User? {
        val authHeader = ctx.header("Authorization") ?: return null
        val (_, token) = authHeader.split(" ")

        return users.getUserByToken(token) ?: return null
    }
}

enum class CheetosRole: Role {
    Public, User
}