package io.andrewohara.cheetosbros.api.games

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.*
import io.andrewohara.cheetosbros.lib.DynamoUtils
import io.andrewohara.cheetosbros.lib.UidConverter
import io.andrewohara.cheetosbros.sources.Achievement

class AchievementsDao(tableName: String, client: AmazonDynamoDB) {

    val mapper = DynamoUtils.mapper<DynamoAchievement, String, String>(tableName, client)

    operator fun get(gameUid: Uid): Collection<Achievement> {
        val query = DynamoDBQueryExpression<DynamoAchievement>()
                .withHashKeyValues(DynamoAchievement(gameUuid = gameUid))

        return mapper.query(query).map { it.toAchievement() }
    }

    fun batchSave(gameId: Uid, achievements: Collection<Achievement>) {
        val items = achievements.map { DynamoAchievement(gameId, it) }
        mapper.batchSave(items)
    }

    @DynamoDBDocument
    data class DynamoAchievement(
            @DynamoDBHashKey
            @DynamoDBTypeConverted(converter = UidConverter::class)
            var gameUuid: Uid? = null,

            @DynamoDBRangeKey
            var achievementId: String? = null,

            var name: String? = null,
            var description: String? = null,
            var hidden: Int? = null,
            var icons: List<String> = emptyList(),
            var score: Int? = null,
    ) {
        constructor(gameId: Uid, achievement: Achievement): this(
                gameUuid = gameId,
                achievementId = achievement.id,
                name = achievement.name,
                description = achievement.description,
                hidden = if (achievement.hidden) 1 else 0,
                icons = achievement.icons,
                score = achievement.score
        )

        fun toAchievement() = Achievement(
                id = achievementId!!,
                name = name!!,
                description = description,
                hidden = hidden == 1,
                icons = icons,
                score = score
        )
    }
}