package io.andrewohara.cheetosbros.api.users

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.*
import io.andrewohara.cheetosbros.lib.DynamoUtils
import io.andrewohara.cheetosbros.sources.Game
import io.andrewohara.cheetosbros.sources.Player

class UsersDao(tableName: String, client: AmazonDynamoDB, private val playersDao: PlayersDao) {

    val mapper = DynamoUtils.mapper<DynamoUser, String, Void>(tableName, client)

    operator fun get(cheetosUserId: String): User? {
        val item = mapper.load(cheetosUserId) ?: return null

        val steamPlayer = item.steamId64?.let { playersDao[Game.Platform.Steam, it] }
        val xboxPlayer = item.xuid?.let { playersDao[Game.Platform.Xbox, it] }

        return item.toUser(xboxPlayer = xboxPlayer, steamPlayer = steamPlayer)
    }

    operator fun get(platform: Game.Platform, socialUserId: String): User? {
        val query = when (platform) {
            Game.Platform.Xbox -> DynamoDBQueryExpression<DynamoUser>()
                    .withIndexName("xuid")
                    .withHashKeyValues(DynamoUser(xuid = socialUserId))
                    .withConsistentRead(false)
            Game.Platform.Steam -> DynamoDBQueryExpression<DynamoUser>()
                    .withIndexName("steamId64")
                    .withHashKeyValues(DynamoUser(steamId64 = socialUserId))
                    .withConsistentRead(false)
        }

        val userId = mapper.query(query).firstOrNull()?.id ?: return null

        return get(userId)
    }

    fun save(user: User) {
        val item = DynamoUser(user)
        mapper.save(item)
    }

    @DynamoDBDocument
    data class DynamoUser(
            @DynamoDBHashKey
            var id: String? = null,
            var displayName: String? = null,

            @DynamoDBIndexHashKey(globalSecondaryIndexName = "xuid")
            var xuid: String? = null,

            @DynamoDBIndexHashKey(globalSecondaryIndexName = "steamId64")
            var steamId64: String? = null,
    ) {
        fun toUser(xboxPlayer: Player?, steamPlayer: Player?) = User(
                id = id!!,
                displayName = displayName!!,
                xbox = xboxPlayer,
                steam = steamPlayer
        )

        constructor(user: User): this(
                id = user.id,
                displayName = user.displayName,
                xuid = user.xbox?.id,
                steamId64 = user.steam?.id,
        )
    }
}
