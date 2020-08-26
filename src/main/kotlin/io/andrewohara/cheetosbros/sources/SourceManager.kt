package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.api.users.User

class SourceManager(private val sourceFactory: SourceFactory) {

    fun getLibrary(user: User, player: Player): Collection<Game> {
        val source = sourceFactory[user, player.platform] ?: return emptyList()

        return source.library(player.id)
    }

    fun getFriends(user: User, player: Player): Collection<String> {
        val source = sourceFactory[user, player.platform] ?: return emptyList()

        return source.getFriends(player.id)
    }

    fun getAchievements(user: User, game: Game): Collection<Achievement> {
        val source = sourceFactory[user, game.platform] ?: return emptyList()

        return source.achievements(game.id)
    }

    fun getAchievementStatus(user: User, game: Game, player: Player): Collection<AchievementStatus> {
        val source = sourceFactory[user, player.platform] ?: return emptyList()

        return source.userAchievements(gameId = game.id, playerId = player.id)
    }
}