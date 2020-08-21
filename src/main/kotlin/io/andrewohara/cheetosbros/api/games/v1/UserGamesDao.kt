package io.andrewohara.cheetosbros.api.games.v1

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.*
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.lib.DynamoUtils
import io.andrewohara.cheetosbros.sources.UserGame
import java.time.Instant

class UserGamesDao(tableName: String, client: AmazonDynamoDB) {

    val mapper = DynamoUtils.mapper<DynamoUserGame, String, String>(tableName, client)

    fun listGameUuids(user: User): Collection<String> {
        val query = DynamoDBQueryExpression<DynamoUserGame>()
                .withHashKeyValues(DynamoUserGame(userId = user.id))
                .withProjectionExpression("gameUuid")

        val result = mapper.query(query)

        return result.mapNotNull { it.gameUuid }
    }

    fun get(user: User, gameUuid: String): UserGame? {
        return mapper.load(user.id, gameUuid)?.toGameStatus()
    }

    fun save(user: User, status: UserGame) {
        val item = DynamoUserGame(user, status)
        mapper.save(item)
    }

    @DynamoDBDocument
    data class DynamoUserGame(
            @DynamoDBHashKey
            var userId: String? = null,

            @DynamoDBRangeKey
            var gameUuid: String? = null,

            var lastPlayed: String? = null
    ) {
        constructor(user: User, status: UserGame): this(
                userId = user.id,
                gameUuid = status.gameUuid,
                lastPlayed = status.lastPlayed?.toString()
        )

        fun toGameStatus() = UserGame(
                gameUuid = gameUuid!!,
                lastPlayed = lastPlayed?.let { Instant.parse(it) }
        )
    }
}