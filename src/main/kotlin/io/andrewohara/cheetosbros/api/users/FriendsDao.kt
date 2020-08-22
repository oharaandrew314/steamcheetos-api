package io.andrewohara.cheetosbros.api.users

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey
import io.andrewohara.cheetosbros.lib.DynamoUtils
import io.andrewohara.cheetosbros.sources.Game

class FriendsDao(tableName: String, client: AmazonDynamoDB) {

    val mapper = DynamoUtils.mapper<DynamoFriend, String, String>(tableName, client)

    operator fun get(user: User, platform: Game.Platform? = null): Set<Friend> {
        val query = DynamoDBQueryExpression<DynamoFriend>()
                .withHashKeyValues(DynamoFriend(userId = user.id))

        return mapper.query(query)
                .filter { platform == null || platform.toString() == it.platform }
                .map { it.toFriend() }
                .toSet()
    }

    fun add(user: User, friend: Friend) {
        val item = DynamoFriend(user, friend)
        mapper.save(item)
    }

    fun remove(user: User, friend: Friend) {
        val item = DynamoFriend(user, friend)
        mapper.delete(item)
    }

    @DynamoDBDocument
    data class DynamoFriend(
            @DynamoDBHashKey
            var userId: String? = null,

            @DynamoDBRangeKey
            var uuid: String? = null,

            var id: String? = null,
            var platform: String? = null
    ) {
        constructor(user: User, friend: Friend): this(
                userId = user.id,
                uuid = friend.uuid,
                id = friend.id,
                platform = friend.platform.toString()
        )

        fun toFriend() = Friend(
                id = id!!,
                platform = Game.Platform.valueOf(platform!!)
        )
    }

    data class Friend(
            val id: String,
            val platform: Game.Platform,
    ) {
        val uuid = "$platform-$id"
    }
}