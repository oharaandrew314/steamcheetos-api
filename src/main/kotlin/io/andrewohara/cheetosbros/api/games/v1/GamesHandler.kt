package io.andrewohara.cheetosbros.api.games.v1

import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.sources.Game
import io.andrewohara.cheetosbros.sources.Source
import java.lang.Exception
import kotlin.concurrent.thread

class GamesHandler(private val steamSource: Source, private val xboxSource: Source) {

    private val games = mutableMapOf<String, Set<Game>>()

    fun listGames(user: User): Collection<Game> = games[user.id] ?: emptySet()

    fun sync(user: User) {
        thread(start = true) {
            println("Syncing games for ${user.displayName}")
            try {
                val userGames = mutableSetOf<Game>()

                user.xbox?.let { xbox ->
                    userGames.addAll(xboxSource.games(xbox.xuid))
                }

                user.steam?.let { steam ->
                    userGames.addAll(steamSource.games(steam.steamId64.toString()))
                }

                games[user.id] = userGames
                println("Synced ${userGames.size} games for ${user.displayName}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}