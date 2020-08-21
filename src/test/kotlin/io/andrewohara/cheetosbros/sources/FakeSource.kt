package io.andrewohara.cheetosbros.sources

import java.lang.IllegalArgumentException

class FakeSource: Source {

    private val players = mutableMapOf<String, Player>()
    private val games = mutableMapOf<String, Game>()

    private val userGames = mutableMapOf<Player, MutableSet<Game>>()
    private val achievements = mutableMapOf<Game, MutableSet<Achievement>>()
    private val userAchievements = mutableMapOf<Pair<Game, Player>, MutableSet<AchievementStatus>>()

    override fun getPlayer(id: String): Player? {
        return players[id]
    }

    fun addPlayer(player: Player) {
        players[player.id] = player
        userGames[player] = mutableSetOf()
    }

    override fun resolveUserId(username: String): String? {
        TODO("Not yet implemented")
    }

    override fun games(userId: String): Collection<Game> {
        val player = getPlayer(userId) ?: throw IllegalArgumentException("Player $userId does not exist")
        return userGames.getValue(player)
    }

    private fun getGame(appId: String): Game {
        return games[appId] ?: throw IllegalArgumentException("Game $appId does not exist")
    }

    fun addGame(game: Game) {
        games[game.id] = game
        achievements[game] = mutableSetOf()
    }

    fun addGameToLibrary(userId: String, appId: String) {
        val player = getPlayer(userId) ?: throw IllegalArgumentException("Player $userId does not exist")
        val game = getGame(appId)

        userGames.getValue(player).add(game)
        userAchievements[game to player] = mutableSetOf()
    }

    override fun achievements(appId: String): Collection<Achievement> {
        val game = getGame(appId)
        return achievements.getValue(game)
    }

    fun addAchievement(appId: String, achievement: Achievement) {
        val game = getGame(appId)
        achievements.getValue(game).add(achievement)
    }

    override fun userAchievements(appId: String, userId: String): Collection<AchievementStatus> {
        val game = getGame(appId)
        val player = getPlayer(userId) ?: throw IllegalArgumentException("Player $userId does not exist")

        return userAchievements.getValue(game to player)
    }

    fun addUserAchievement(appId: String, userId: String, achievementStatus: AchievementStatus) {
        val game = getGame(appId)
        val player = getPlayer(userId) ?: throw IllegalArgumentException("Player $userId does not exist")

        userAchievements.getValue(game to player).add(achievementStatus)
    }
}