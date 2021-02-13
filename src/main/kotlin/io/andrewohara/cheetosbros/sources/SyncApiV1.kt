package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.api.users.UsersManager
import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse

class SyncApiV1(app: Javalin, private val users: UsersManager, private val sources: SourceManager) {

    init {
        app.post("/v1/sync/:platform", ::manualSync)
    }

    private fun manualSync(context: Context) {
        val user = context.attribute<User>("user")!!
        val platform = context.pathParam<Platform>("platform").get()

        val player = users.getPlayer(user, platform) ?: throw NotFoundResponse("No $platform player for ${user.displayName}")

        sources.sync(user, player)
    }
}