package io.andrewohara.cheetosbros.api.games.v1

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.*
import io.andrewohara.cheetosbros.lib.DynamoUtils
import io.andrewohara.cheetosbros.lib.PlatformConverter
import io.andrewohara.cheetosbros.sources.Player
import io.andrewohara.cheetosbros.sources.Platform

class GameLibraryDao(tableName: String, client: AmazonDynamoDB) {

    val mapper = DynamoUtils.mapper<DynamoOwnedGame, String, String>(tableName, client)

    operator fun get(player: Player): Collection<OwnedGame> {
        val query = DynamoDBQueryExpression<DynamoOwnedGame>()
            .withHashKeyValues(DynamoOwnedGame(playerUuid = player.uuid()))

        return mapper.query(query).map { it.toModel() }
    }

    operator fun get(player: Player, gameId: String): OwnedGame? {
        return mapper.load(player.uuid(), gameId)?.toModel()
    }

    fun save(player: Player, ownedGame: OwnedGame) {
        val item = DynamoOwnedGame(player, ownedGame)

        mapper.save(item)
    }

    @DynamoDBDocument
    data class DynamoOwnedGame(
        @DynamoDBHashKey
        var playerUuid: String? = null,

        @DynamoDBRangeKey
        var gameId: String? = null,

        @DynamoDBTypeConverted(converter = PlatformConverter::class)
        var platform: Platform? = null,
        var name: String? = null,
        var currentAchievements: Int? = null,
        var totalAchievements: Int? = null,
        var displayImage: String? = null
    ) {
        constructor(player: Player, ownedGame: OwnedGame) : this(
            playerUuid = "${player.platform}-${player.id}",
            gameId = ownedGame.id,
            platform = player.platform,
            name = ownedGame.name,
            displayImage = ownedGame.displayImage,
            currentAchievements = ownedGame.currentAchievements,
            totalAchievements = ownedGame.totalAchievements
        )

        fun toModel() = OwnedGame(
            platform = platform!!,
            id = gameId!!,
            name = name!!,
            currentAchievements = currentAchievements!!,
            totalAchievements = totalAchievements!!,
            displayImage = displayImage
        )
    }

    companion object {
        private fun Player.uuid() = "$platform-$id"
    }
}