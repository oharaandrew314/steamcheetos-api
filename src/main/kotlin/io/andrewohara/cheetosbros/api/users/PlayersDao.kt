package io.andrewohara.cheetosbros.api.users

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.*
import io.andrewohara.cheetosbros.lib.DynamoUtils
import io.andrewohara.cheetosbros.lib.PlatformConverter
import io.andrewohara.cheetosbros.sources.Platform
import io.andrewohara.cheetosbros.sources.Player

class PlayersDao(tableName: String, client: AmazonDynamoDB) {

    val mapper = DynamoUtils.mapper<DynamoPlayer, String, Void>(tableName, client)

    fun save(player: Player) {
        mapper.save(DynamoPlayer(player))
    }

    fun listForUser(user: User): Collection<Player> {
        val query = DynamoDBQueryExpression<DynamoPlayer>()
                .withIndexName("users")
                .withHashKeyValues(DynamoPlayer(userId = user.id))
                .withConsistentRead(false)

        return mapper.query(query).map { it.toPlayer() }
    }

    fun getUserId(player: Player): String? {
        return mapper.load(uuid(player))?.userId
    }

    fun linkUser(player: Player, user: User) {
        val item = DynamoPlayer(player).copy(userId = user.id)
        mapper.save(item)
    }

    private fun batchGet(platform: Platform, ids: Collection<String>): Collection<Player> {
        val keys = ids.map { DynamoPlayer(uuid = uuid(platform, it)) }
        return mapper.batchLoad(keys).map { it.toPlayer() }
    }

    operator fun get(platform: Platform, playerId: String): Player? {
        return mapper.load(uuid(platform, playerId))?.toPlayer()
    }

    fun getFriends(player: Player): Collection<Player>? {
        val friendIds = mapper.load(uuid(player))?.friendIds ?: return null

        val friends = batchGet(player.platform, friendIds)
                .map { it.id to it }
                .toMap()

        return friendIds.map { id ->
            friends[id] ?: Player(platform = player.platform, id = id, username = "Unknown user", avatar = null)
        }
    }

    fun saveFriends(player: Player, friendIds: Collection<String>) {
        val item = mapper.load(uuid(player)) ?: return
        item.friendIds = friendIds.toList()

        mapper.save(item)
    }

    @DynamoDBDocument
    data class DynamoPlayer(
            @DynamoDBHashKey
            var uuid: String? = null,

            @DynamoDBIndexHashKey(globalSecondaryIndexName = "users")
            var userId: String? = null,

            var id: String? = null,
            @DynamoDBTypeConverted(converter = PlatformConverter::class) var platform: Platform? = null,
            var username: String? = null,
            var avatar: String? = null,

            var friendIds: List<String> = emptyList(),
    ) {
        constructor(player: Player): this(
            uuid = "${player.platform}-${player.id}",
            id = player.id,
            platform = player.platform,
            username = player.username,
            avatar = player.avatar
        )

        fun toPlayer() = Player(
                id = id!!,
                platform = platform!!,
                username = username!!,
                avatar = avatar
        )
    }

    companion object {
        private fun uuid(player: Player) = uuid(player.platform, player.id)
        private fun uuid(platform: Platform, id: String) = "$platform-$id"
    }
}