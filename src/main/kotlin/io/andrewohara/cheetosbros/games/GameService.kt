package io.andrewohara.cheetosbros.games

class GameService(
    private val gamesDao: GamesDao,
    private val achievementsDao: AchievementsDao,
) {
    fun listGames(userId: String): Collection<Game> {
        return gamesDao[userId]
    }

    fun getGame(userId: String, gameId: String): Game? = gamesDao[userId, gameId]

    fun listAchievements(userId: String, gameId: String): Collection<Achievement> {
        return achievementsDao[userId, gameId]
    }

    fun update(game: Game, achievements: Collection<Achievement>) {
        gamesDao += game
        achievementsDao += achievements
    }
}