package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.api.games.v1.*
import org.slf4j.LoggerFactory
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class SourceManager(
    private val sourceFactory: SourceFactory,
    private val gamesDao: GamesDao,
    private val gameLibraryDao: GameLibraryDao,
    private val achievementsDao: AchievementsDao,
    private val achievementStatusDao: AchievementStatusDao,
    private val syncExecutor: Executor = Executors.newSingleThreadExecutor()
    ) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun sync(player: Player) {
        val source = sourceFactory[player] ?: let {
            log.warn("No ${player.platform} source found for ${player.username}")
            return
        }

        sync(player, source)
    }

    fun sync(player: Player, source: Source) {
        log.info("Action=SyncPlayerStart Player=${player.platform}-${player.username}")

        val games = source.library(player.id)

        for (game in games) {
            syncExecutor.execute {
                syncGame(source, player, game)
                Thread.sleep(1000)
            }
        }

        log.info("Action=SyncPlayerComplete Player=${player.platform}-${player.username} Games=${games.size}")
    }

    private fun syncGame(source: Source, player: Player, game: Game) {
        log.debug("Action=SyncGameStart Player=${player.platform}-${player.username} Game=${game.platform}-${game.id}")

        val existingGame = gamesDao[game.platform, game.id]
        val numAchievements = if (existingGame == null) {
            gamesDao.save(game)
            val retrieved = source.achievements(game.id)
            achievementsDao.batchSave(game, retrieved)
            retrieved.size
        } else achievementsDao.countAchievements(existingGame)


        val numCompleted = if (numAchievements > 0) {
            val retrieved = source.userAchievements(game.id, player.id)
            achievementStatusDao.batchSave(player, game, retrieved)
            retrieved.count { it.unlockedOn != null }
        } else 0

        gameLibraryDao.save(player, OwnedGame(game.platform, game.id, game.name, currentAchievements = numCompleted, totalAchievements = numAchievements, displayImage = game.displayImage))

        log.debug("Action=SyncGameComplete Player=${player.platform}-${player.username} Game=${game.platform}-${game.id} Achievements=$numAchievements")
    }
}