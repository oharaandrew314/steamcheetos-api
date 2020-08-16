package io.andrewohara.cheetosbros.api.games.v1

import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.sources.Game
import io.andrewohara.cheetosbros.sources.Source

class GamesHandler(private val steamSource: Source, private val xboxSource: Source) {

    fun listGames(user: User): Collection<Game> {
        val games = mutableSetOf<Game>()

        if (user.xbox != null) {
            games.addAll(xboxSource.games(user.xbox.xuid))
        }

        if (user.steam != null) {
            games.addAll(steamSource.games(user.steam.steamId64.toString()))
        }

        return games
    }
}