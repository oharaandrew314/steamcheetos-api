package io.andrewohara.cheetosbros.api.games

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey
import io.andrewohara.cheetosbros.lib.DynamoUtils
import io.andrewohara.cheetosbros.sources.AchievementStatus
import java.time.Instant

class AchievementStatusDao(tableName: String, client: AmazonDynamoDB) {
    val mapper = DynamoUtils.mapper<DynamoUserAchievement, String, String>(tableName, client)

    fun batchSave(playerUid: Uid, gameUid: Uid, achievementStatuses: Collection<AchievementStatus>) {
        val items = achievementStatuses.map { DynamoUserAchievement(playerUid, gameUid, it) }
        mapper.batchSave(items)
    }

    operator fun get(playerUid: Uid, gameUid: Uid): Collection<AchievementStatus> {
        val query = DynamoDBQueryExpression<DynamoUserAchievement>()
                .withHashKeyValues(DynamoUserAchievement(playerAndGameId = hashKey(playerUid, gameUid)))

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
        constructor(playerUid: Uid, gameUid: Uid, achievementStatus: AchievementStatus): this(
                playerAndGameId = hashKey(playerUid, gameUid),
                achievementId = achievementStatus.achievementId,
                unlockedOn = achievementStatus.unlockedOn?.toString(),
        )

        fun toStatus() = AchievementStatus(
                achievementId = achievementId!!,
                unlockedOn = unlockedOn?.let { Instant.parse(it) },
        )
    }

    companion object {
        fun hashKey(playerUid: Uid, gameUid: Uid) = "$playerUid-${gameUid.id}"
    }
}