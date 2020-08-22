package io.andrewohara.cheetosbros.api.games.v1

import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.sources.Achievement
import io.andrewohara.cheetosbros.sources.Game
import io.andrewohara.cheetosbros.sources.AchievementStatus
import java.time.Duration
import java.time.Instant

class GamesManager(
        private val gamesDao: GamesDao,
        private val userGamesDao: UserGamesDao,
        private val achievementsDao: AchievementsDao,
        private val achievementStatusDao: AchievementStatusDao
) {
    fun listGames(user: User): Collection<Game> {
        val start = Instant.now()

        val gameUuids = userGamesDao.listGameUuids(user)
        println("Got game uuids in ${Duration.between(start, Instant.now())}")

        val games = gamesDao.batchGet(gameUuids)
        println("Got games in ${Duration.between(start, Instant.now())}")
        return games
    }

    fun listAchievements(user: User, gameUuid: String): Collection<AchievementDetails>? {
        val game = gamesDao[gameUuid] ?: return null

        val achievements = achievementsDao.list(game)

        val status = achievementStatusDao.list(user, game)
                .map { it.achievementId to it }
                .toMap()

        return achievements.map {
            AchievementDetails(
                    achievement = it,
                    status = status[it.id] ?: AchievementStatus(achievementId = it.id, unlockedOn = null)
            )
        }
    }

    fun getGame(gameUuid: String): Game? {
        return gamesDao[gameUuid]
    }

    data class AchievementDetails(
            val achievement: Achievement,
            val status: AchievementStatus
    )
}