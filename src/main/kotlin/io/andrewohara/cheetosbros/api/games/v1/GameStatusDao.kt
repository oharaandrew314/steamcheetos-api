package io.andrewohara.cheetosbros.api.games.v1

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.*
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.sources.AchievementStatus
import io.andrewohara.cheetosbros.sources.GameStatus
import java.time.Instant

class GameStatusDao(tableName: String, client: AmazonDynamoDB? = null) {

    private val mapper = let {
        val config = DynamoDBMapperConfig.Builder()
                .withTableNameOverride(DynamoDBMapperConfig.TableNameOverride.withTableNameReplacement(tableName))
                .build()

        DynamoDBMapper(client ?: AmazonDynamoDBClientBuilder.defaultClient(), config)
                .newTableMapper<DynamoGameStatus, String, String>(DynamoGameStatus::class.java)
    }

    fun createTableIfNotExists() {
        mapper.createTableIfNotExists(ProvisionedThroughput(1, 1))
    }

    fun list(user: User): Collection<GameStatus> {
        val query = DynamoDBQueryExpression<DynamoGameStatus>()
                .withHashKeyValues(DynamoGameStatus(userId = user.id))

        val result = mapper.query(query)

        return result.map { it.toGameStatus() }
    }

    fun save(user: User, status: GameStatus) {
        val item = DynamoGameStatus(user, status)
        mapper.save(item)
    }

    @DynamoDBDocument
    data class DynamoGameStatus(
            @DynamoDBHashKey
            var userId: String? = null,

            @DynamoDBRangeKey
            var gameUuid: String? = null,

            var achievements: List<DynamoAchievementStatus> = emptyList()
    ) {
        constructor(user: User, status: GameStatus): this(
                userId = user.id,
                gameUuid = status.gameUuid,
                achievements = status.achievements.map { DynamoAchievementStatus(it) }
        )

        fun toGameStatus() = GameStatus(
                gameUuid = gameUuid!!,
                achievements = achievements.map { it.toStatus() }
        )
    }

    @DynamoDBDocument
    data class DynamoAchievementStatus(
            var id: String? = null,
            var unlockedOn: String? = null
    ) {
        constructor(achievement: AchievementStatus): this(
            id = achievement.id,
            unlockedOn = achievement.unlockedOn?.toString()
        )

        fun toStatus() = AchievementStatus(
                id = id!!,
                unlockedOn = unlockedOn?.let { Instant.parse(it) }
        )
    }
}