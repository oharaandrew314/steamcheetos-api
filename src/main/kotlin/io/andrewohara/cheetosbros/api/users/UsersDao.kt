package io.andrewohara.cheetosbros.api.users

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.*
import io.andrewohara.cheetosbros.lib.DynamoUtils
import io.andrewohara.cheetosbros.lib.PlatformConverter
import io.andrewohara.cheetosbros.sources.Platform
import io.andrewohara.cheetosbros.sources.Player

class UsersDao(tableName: String, client: AmazonDynamoDB) {

    val mapper = DynamoUtils.mapper<DynamoUser, String, Void>(tableName, client)

    operator fun get(cheetosUserId: String): User? {
        return mapper.load(cheetosUserId)?.toUser()
    }

    fun save(user: User) {
        val item = DynamoUser(user)
        mapper.save(item)
    }

    @DynamoDBDocument
    data class DynamoUser(
            @DynamoDBHashKey
            var id: String? = null,
            var players: List<DynamoPlayer> = emptyList()
    ) {
        fun toUser() = User(
                id = id!!,
                players = players
                    .map { it.platform!! to it.toPlayer() }
                    .toMap()
        )

        constructor(user: User): this(
                id = user.id,
                players = user.players.values.map { DynamoPlayer(it) }
        )
    }

    @DynamoDBDocument
    data class DynamoPlayer(
        var id: String? = null,
        @DynamoDBTypeConverted(converter = PlatformConverter::class)
        var platform: Platform? = null,
        var username: String? = null,
        var avatar: String? = null,
        var token: String? = null
    ) {
        constructor(player: Player): this(
            id = player.id,
            platform = player.platform,
            username = player.username,
            avatar = player.avatar,
            token = player.token
        )

        fun toPlayer() = Player(
            id = id!!,
            platform = platform!!,
            username = username!!,
            avatar = avatar,
            token = token
        )
    }
}
