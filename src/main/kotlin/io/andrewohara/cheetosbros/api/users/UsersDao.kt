package io.andrewohara.cheetosbros.api.users

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.*
import io.andrewohara.cheetosbros.api.games.Uid
import io.andrewohara.cheetosbros.lib.DynamoUtils
import io.andrewohara.cheetosbros.lib.PlatformConverter
import io.andrewohara.cheetosbros.lib.UUIDConverter
import io.andrewohara.cheetosbros.sources.Platform
import io.andrewohara.cheetosbros.sources.Player
import java.util.*

class UsersDao(tableName: String, client: AmazonDynamoDB) {

    val mapper = DynamoUtils.mapper<DynamoUser, UUID, Void>(tableName, client)

    operator fun get(uuid: UUID): User? {
        return mapper.load(uuid)?.toUser()
    }

    fun save(user: User) {
        val item = DynamoUser(user)
        mapper.save(item)
    }

    @DynamoDBDocument
    data class DynamoUser(
            @DynamoDBHashKey
            @DynamoDBTypeConverted(converter = UUIDConverter::class)
            var id: UUID? = null,

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
            id = player.uid.id,
            platform = player.uid.platform,
            username = player.username,
            avatar = player.avatar,
            token = player.token
        )

        fun toPlayer() = Player(
            uid = Uid(platform!!, id!!),
            username = username!!,
            avatar = avatar,
            token = token
        )
    }
}
