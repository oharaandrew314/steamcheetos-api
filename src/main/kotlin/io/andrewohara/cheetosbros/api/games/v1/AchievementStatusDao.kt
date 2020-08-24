package io.andrewohara.cheetosbros.api.games.v1

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey
import io.andrewohara.cheetosbros.lib.DynamoUtils
import io.andrewohara.cheetosbros.sources.AchievementStatus
import io.andrewohara.cheetosbros.sources.Game
import io.andrewohara.cheetosbros.sources.Player
import java.time.Instant

class AchievementStatusDao(tableName: String, client: AmazonDynamoDB) {
    val mapper = DynamoUtils.mapper<DynamoUserAchievement, String, String>(tableName, client)

    fun batchSave(player: Player, game: Game, achievementStatuses: Collection<AchievementStatus>) {
        val items = achievementStatuses.map { DynamoUserAchievement(player, game, it) }
        mapper.batchSave(items)
    }

    fun list(player: Player, game: Game): Collection<AchievementStatus> {
        val query = DynamoDBQueryExpression<DynamoUserAchievement>()
                .withHashKeyValues(DynamoUserAchievement(playerAndGameId = hashKey(player, game)))

        return mapper.query(query).map { it.toStatus() }
    }

    @DynamoDBDocument
    data class DynamoUserAchievement(
            @DynamoDBHashKey
            var playerAndGameId: String? = null,

            @DynamoDBRangeKey
            var achievementId: String? = null,

            var unlockedOn: String? = null,
    ) {
        constructor(player: Player, game: Game, achievementStatus: AchievementStatus): this(
                playerAndGameId = hashKey(player, game),
                achievementId = achievementStatus.achievementId,
                unlockedOn = achievementStatus.unlockedOn?.toString(),
        )

        fun toStatus() = AchievementStatus(
                achievementId = achievementId!!,
                unlockedOn = unlockedOn?.let { Instant.parse(it) },
        )
    }

    companion object {
        fun hashKey(player: Player, game: Game) = "${player.platform}-${player.id}-${game.id}"
    }
}