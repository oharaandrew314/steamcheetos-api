package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.api.games.*
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import java.time.Duration
import java.time.Instant

class SourceManager(
    private val sourceFactory: SourceFactory,
    private val gamesDao: GamesDao,
    private val gameLibraryDao: GameLibraryDao,
    private val achievementsDao: AchievementsDao,
    private val achievementStatusDao: AchievementStatusDao,
    private val gameCacheDuration: Duration,
    private val timeSupplier: () -> Instant
    ) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun discoverGames(player: Player): Collection<Source.Game> {
        log.info("Action=DiscoverGames Player=${player.uid}")

        val source = sourceFactory[player] ?: throw IllegalArgumentException("No source found for player: ${player.username}")

        val games = source.library(player.uid.id)

        log.info("Action=DiscoverGames Player=${player.uid} Games=${games.size}")

        return games
    }

    fun syncGame(player: Player, discovered: Source.Game) {
        log.debug("Action=SyncGameStart Player=${player.uid} Game=${discovered.uid}")

        val time = timeSupplier()
        val source = sourceFactory[player] ?: throw IllegalArgumentException("No source found for player: ${player.username}")

        val game = let {
            val existing = gamesDao[discovered.uid]
            if (existing == null || Duration.between(existing.lastUpdated, time) > gameCacheDuration) {
                val achievements = source.achievements(discovered.uid.id)
                achievementsDao.batchSave(discovered.uid, achievements)

                val game = CachedGame.create(discovered, achievements.size, time)
                gamesDao.save(game)
                game
            } else existing
        }


        val numCompleted = if (game.achievements > 0) {
            val progress = source.userAchievements(discovered.uid.id, player.uid.id)
            achievementStatusDao.batchSave(player.uid, discovered.uid, progress)
            progress.count { it.unlockedOn != null }
        } else 0

        gameLibraryDao.save(player, OwnedGame(discovered.uid, numCompleted, time))

        log.debug("Action=SyncGameComplete Player=${player.uid} Game=${discovered.uid} Achievements=${game.achievements}")
    }
}