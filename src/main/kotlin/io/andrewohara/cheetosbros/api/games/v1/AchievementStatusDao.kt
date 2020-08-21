package io.andrewohara.cheetosbros.api.games.v1

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.lib.DynamoUtils
import io.andrewohara.cheetosbros.sources.AchievementStatus
import io.andrewohara.cheetosbros.sources.Game
import java.time.Instant

class AchievementStatusDao(tableName: String, client: AmazonDynamoDB) {
    val mapper = DynamoUtils.mapper<DynamoUserAchievement, String, String>(tableName, client)

    fun batchSave(user: User, game: Game, achievementStatuses: Collection<AchievementStatus>) {
        val items = achievementStatuses.map { DynamoUserAchievement(user, game, it) }
        mapper.batchSave(items)
    }

    fun list(user: User, game: Game): Collection<AchievementStatus> {
        val query = DynamoDBQueryExpression<DynamoUserAchievement>()
                .withHashKeyValues(DynamoUserAchievement(userIdAndGameId = "${user.id}-${game.uuid}"))

        return mapper.query(query).map { it.toStatus() }
    }

    @DynamoDBDocument
    data class DynamoUserAchievement(
            @DynamoDBHashKey
            var userIdAndGameId: String? = null,

            @DynamoDBRangeKey
            var achievementId: String? = null,

            var unlockedOn: String? = null,
    ) {
        constructor(user: User, game: Game, achievementStatus: AchievementStatus): this(
                userIdAndGameId = "${user.id}-${game.uuid}",
                achievementId = achievementStatus.achievementId,
                unlockedOn = achievementStatus.unlockedOn?.toString(),
        )

        fun toStatus() = AchievementStatus(
                achievementId = achievementId!!,
                unlockedOn = unlockedOn?.let { Instant.parse(it) },
        )
    }
}