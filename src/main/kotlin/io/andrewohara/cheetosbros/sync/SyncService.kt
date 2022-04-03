package io.andrewohara.cheetosbros.sync

import io.andrewohara.cheetosbros.games.*
import io.andrewohara.cheetosbros.sources.UserData
import io.andrewohara.cheetosbros.sources.steam.SteamClient
import java.time.Clock

class SyncService(
    private val steam: SteamClient,
    private val gameService: GameService,
    private val clock: Clock
) {

    fun getUser(userId: String): UserData? {
        return steam.getPlayer(userId.toLong())
    }

    fun discoverGames(userId: String): Collection<String> {
        return steam.listGameIds(userId.toLong())
    }

    fun syncGame(userId: String, steamId: String): GameSyncResult? {
        val gameData = steam.getGameData(steamId.toLong()) ?: return null
        val achievementData = steam.achievements(steamId.toLong())
        if (achievementData.isEmpty()) return null  // don't sync game without achievements

        val progressData = steam.userAchievements(gameId = steamId.toLong(), playerId = userId.toLong())
            .associateBy { it.achievementId }

        val game = Game(
            userId = userId,
            id = gameData.id,
            name = gameData.name,
            displayImage = gameData.displayImage,
            lastUpdated = clock.instant(),
            achievementsTotal = progressData.size,
            achievementsUnlocked = progressData.values.count { it.unlockedOn != null },
        )

        val achievements = achievementData.map { data ->
            Achievement(
                libraryId = LibraryId(userId, data.gameId),
                id = data.id,
                name = data.name,
                description = data.description,
                hidden = data.hidden,
                iconLocked = data.iconLocked,
                iconUnlocked = data.iconUnlocked,
                score = data.score,
                unlockedOn = progressData[data.id]?.unlockedOn
            )
        }

        gameService.update(game, achievements)

        return GameSyncResult(game, achievements.toSet())
    }
}

data class GameSyncResult(val game: Game, val achievements: Set<Achievement>)