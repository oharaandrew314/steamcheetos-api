package io.andrewohara.cheetosbros.api.users.v1

import io.andrewohara.cheetosbros.api.auth.CheetosRole
import io.andrewohara.cheetosbros.api.users.User
import io.javalin.Javalin
import io.javalin.core.security.SecurityUtil
import io.javalin.http.Context

class UsersControllerV1 {

    fun register(app: Javalin) {
        app.get("/v1/users/profile", ::profile, SecurityUtil.roles(CheetosRole.User))
    }

    private fun profile(ctx: Context) {
        val user = ctx.attribute<User>("user")!!

        val response = UserResponseV1(
                id = user.id,
                displayName = user.displayName,
                steamUsername = user.steam?.username,
                xboxGamerTag = user.xbox?.gamertag
        )

        ctx.json(response)
    }
}

data class UserResponseV1(
        val id: String,
        val displayName: String,
        val xboxGamerTag: String?,
        val steamUsername: String?
)