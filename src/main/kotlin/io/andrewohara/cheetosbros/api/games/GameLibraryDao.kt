package io.andrewohara.cheetosbros.api.games

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.*
import io.andrewohara.cheetosbros.lib.DynamoUtils
import io.andrewohara.cheetosbros.lib.IsoInstantConverter
import io.andrewohara.cheetosbros.lib.UidConverter
import io.andrewohara.cheetosbros.sources.Player
import java.time.Instant

class GameLibraryDao(tableName: String, client: AmazonDynamoDB) {

    val mapper = DynamoUtils.mapper<DynamoOwnedGame, Uid, String>(tableName, client)

    operator fun get(player: Player): Collection<OwnedGame> {
        val query = DynamoDBQueryExpression<DynamoOwnedGame>()
            .withHashKeyValues(DynamoOwnedGame(playerUuid = player.uid))

        return mapper.query(query).map { it.toModel() }
    }

    operator fun get(player: Player, gameId: String): OwnedGame? {
        return mapper.load(player.uid, gameId)?.toModel()
    }

    fun save(player: Player, ownedGame: OwnedGame) {
        val item = DynamoOwnedGame(player, ownedGame)

        mapper.save(item)
    }

    @DynamoDBDocument
    data class DynamoOwnedGame(
        @DynamoDBHashKey
        @DynamoDBTypeConverted(converter = UidConverter::class)
        var playerUuid: Uid? = null,

        @DynamoDBRangeKey
        var gameId: String? = null,

        var achievements: Int? = null,

        @DynamoDBTypeConverted(converter = IsoInstantConverter::class)
        var lastUpdated: Instant? = null
    ) {
        constructor(player: Player, ownedGame: OwnedGame) : this(
            playerUuid = player.uid,
            gameId = ownedGame.uid.id,
            achievements = ownedGame.achievements,
            lastUpdated = ownedGame.lastUpdated
        )

        fun toModel() = OwnedGame(
            uid = Uid(playerUuid!!.platform, gameId!!),
            achievements = achievements ?: 0,
            lastUpdated = lastUpdated ?: Instant.MIN
        )
    }
}