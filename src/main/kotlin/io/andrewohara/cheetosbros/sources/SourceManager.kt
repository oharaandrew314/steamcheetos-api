package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.api.games.v1.*
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException

class SourceManager(
    private val sourceFactory: SourceFactory,
    private val gamesDao: GamesDao,
    private val gameLibraryDao: GameLibraryDao,
    private val achievementsDao: AchievementsDao,
    private val achievementStatusDao: AchievementStatusDao
    ) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun discoverGames(player: Player): Collection<Game> {
        log.info("Action=DiscoverGames Player=${player.uid()}")

        val source = sourceFactory[player] ?: throw IllegalArgumentException("No source found for player: ${player.username}")

        val games = source.library(player.id)

        log.info("Action=DiscoverGames Player=${player.uid()} Games=${games.size}")

        return games
    }

    fun syncGame(player: Player, game: Game) {
        log.debug("Action=SyncGameStart Player=${player.uid()} Game=${game.uid()}")

        val source = sourceFactory[player] ?: throw IllegalArgumentException("No source found for player: ${player.username}")

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

        log.debug("Action=SyncGameComplete Player=${player.uid()} Game=${game.uid()} Achievements=$numAchievements")
    }
}