package io.andrewohara.cheetosbros.api.users

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import io.andrewohara.cheetosbros.lib.DynamoUtils
import io.andrewohara.cheetosbros.sources.Game
import io.andrewohara.cheetosbros.sources.Player

class PlayersDao(tableName: String, client: AmazonDynamoDB) {

    val mapper = DynamoUtils.mapper<DynamoPlayer, String, Void>(tableName, client)

    fun save(player: Player) {
        val item = DynamoPlayer(player)
        mapper.save(item)
    }

    operator fun get(platform: Game.Platform, id: String): Player? {
        return mapper.load("$platform-$id")?.toPlayer()
    }

    fun batchGet(uuids: Collection<String>): Collection<Player> {
        return mapper.batchLoad(uuids.map { DynamoPlayer(uuid = it) }).map { it.toPlayer() }
    }

    @DynamoDBDocument
    data class DynamoPlayer(
            @DynamoDBHashKey
            var uuid: String? = null,

            var id: String? = null,
            var platform: String? = null,
            var displayName: String? = null,
            var avatar: String? = null
    ) {
        constructor(player: Player): this(
            uuid = "${player.platform}-${player.id}",
            id = player.id,
            platform = player.platform.toString(),
            displayName = player.username,
            avatar = player.avatar
        )

        fun toPlayer() = Player(
                id = id!!,
                platform = Game.Platform.valueOf(platform!!),
                username = displayName!!,
                avatar = avatar
        )
    }
}