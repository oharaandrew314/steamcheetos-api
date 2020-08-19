package io.andrewohara.cheetosbros.api.games.v1

import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.sources.Game
import io.andrewohara.cheetosbros.sources.Source

class GamesHandler(private val steamSource: Source, private val xboxSource: Source) {

    fun listGames(user: User): Collection<Game> {
        val games = mutableSetOf<Game>()

        user.xbox?.let { xbox ->
            games.addAll(xboxSource.games(xbox.xuid))
        }

        user.steam?.let { steam ->
            games.addAll(steamSource.games(steam.steamId64.toString()))
        }

        return games
    }
}