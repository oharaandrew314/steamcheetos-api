package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.api.games.v1.AchievementStatusDao
import io.andrewohara.cheetosbros.api.games.v1.AchievementsDao
import io.andrewohara.cheetosbros.api.games.v1.GameLibraryDao
import io.andrewohara.cheetosbros.api.games.v1.GamesDao
import io.andrewohara.cheetosbros.api.users.User
import org.slf4j.LoggerFactory

class SourceManager(
    private val sourceFactory: SourceFactory,
    private val gamesDao: GamesDao,
    private val gameLibraryDao: GameLibraryDao,
    private val achievementsDao: AchievementsDao,
    private val achievementStatusDao: AchievementStatusDao
    ) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun sync(user: User, player: Player) {
        val source = sourceFactory[user, player.platform] ?: let {
            log.warn("No ${player.platform} source found for ${user.displayName}")
            return
        }

        sync(player, source)
    }

    fun sync(player: Player, source: Source) {
        log.info("Sync Games for $player")

        val games = source.library(player.id)
        gamesDao.batchSave(games)
        gameLibraryDao.batchSave(player, games.map { LibraryItem(it) })
        log.debug("Synced ${games.size} ${player.platform} for $player")

        for (game in games) {
            // TODO come up with way to know if not needed to sync achievements
            val achievements = source.achievements(game.id)
            achievementsDao.batchSave(game, achievements)
            log.debug("Synced ${achievements.size} achievements for ${game.platform} ${game.name}")

            if (achievements.isNotEmpty()) {
                val status = source.userAchievements(game.id, player.id)
                achievementStatusDao.batchSave(player, game, status)
                log.debug("Synced ${status.size} achievement statuses for ${player.platform} ${player.username} for ${game.name}")
            }
        }
    }
}