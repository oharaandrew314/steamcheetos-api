package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.api.auth.CheetosRole
import io.andrewohara.cheetosbros.api.users.User
import io.javalin.Javalin
import io.javalin.core.security.SecurityUtil
import io.javalin.http.Context

class SyncApiV1(app: Javalin, private val sources: SourceManager) {

    init {
        app.post("/v1/sync", ::sync, SecurityUtil.roles(CheetosRole.User))
    }

    private fun sync(context: Context) {
        val user = context.attribute<User>("user")!!

        for (player in user.players.values) {
            sources.sync(player)
        }
    }
}