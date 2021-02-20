package io.andrewohara.cheetosbros.sources

import java.lang.IllegalArgumentException

class FakeSource(override val platform: Platform): Source {

    private val players = mutableMapOf<String, Player>()
    private val games = mutableMapOf<String, Game>()

    private val userGames = mutableMapOf<Player, MutableSet<Game>>()
    private val achievements = mutableMapOf<Game, MutableSet<Achievement>>()
    private val userAchievements = mutableMapOf<Pair<Game, Player>, MutableSet<AchievementStatus>>()

    private fun getPlayer(playerId: String): Player? {
        return players[playerId]
    }

    fun addPlayer(player: Player) {
        players[player.id] = player
        userGames[player] = mutableSetOf()
    }

    override fun library(playerId: String): Collection<Game> {
        val player = getPlayer(playerId) ?: throw IllegalArgumentException("Player $playerId does not exist")
        return userGames.getValue(player)
    }

    private fun game(appId: String): Game {
        return games[appId] ?: throw IllegalArgumentException("Game $appId does not exist")
    }

    fun addGame(game: Game) {
        games[game.id] = game
        achievements[game] = mutableSetOf()
    }

    fun addGameToLibrary(playerId: String, appId: String) {
        val player = getPlayer(playerId) ?: throw IllegalArgumentException("Player $playerId does not exist")
        val game = game(appId)

        userGames.getValue(player).add(game)
        userAchievements[game to player] = mutableSetOf()
    }

    override fun achievements(gameId: String): Collection<Achievement> {
        val game = game(gameId)
        return achievements.getValue(game)
    }

    fun addAchievement(appId: String, achievement: Achievement) {
        val game = game(appId)
        achievements.getValue(game).add(achievement)
    }

    override fun userAchievements(gameId: String, playerId: String): Collection<AchievementStatus> {
        val game = game(gameId)
        val player = getPlayer(playerId) ?: throw IllegalArgumentException("Player $playerId does not exist")

        val achievements = achievements.getValue(game)

        val statuses = userAchievements.getValue(game to player)
            .map { it.achievementId to it }
            .toMap()

        return achievements.map { statuses[it.id] ?: AchievementStatus(it.id, null) }
    }

    fun addUserAchievement(gameId: String, userId: String, achievementStatus: AchievementStatus) {
        val game = game(gameId)
        val player = getPlayer(userId) ?: throw IllegalArgumentException("Player $userId does not exist")

        userAchievements.getValue(game to player).add(achievementStatus)
    }

    override fun getFriends(playerId: String) = emptySet<String>()

    fun clear() {
        players.clear()
        games.clear()
        userGames.clear()
        achievements.clear()
        userAchievements.clear()
    }
}