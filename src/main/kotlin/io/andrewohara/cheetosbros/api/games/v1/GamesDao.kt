package io.andrewohara.cheetosbros.api.games.v1

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import io.andrewohara.cheetosbros.sources.Achievement
import io.andrewohara.cheetosbros.sources.Game
import io.andrewohara.cheetosbros.sources.GameDetails

class GamesDao(tableName: String, client: AmazonDynamoDB? = null) {

    private val mapper = let {
        val config = DynamoDBMapperConfig.Builder()
                .withTableNameOverride(DynamoDBMapperConfig.TableNameOverride.withTableNameReplacement(tableName))
                .build()

         DynamoDBMapper(client ?: AmazonDynamoDBClientBuilder.defaultClient(), config)
                .newTableMapper<DynamoGame, String, String>(DynamoGame::class.java)
    }

    fun createTableIfNotExists() {
        mapper.createTableIfNotExists(ProvisionedThroughput(1, 1))
    }

    fun save(game: GameDetails) {
        val item = DynamoGame(game.game, game.achievements)
        mapper.save(item)
    }

    operator fun get(uuid: String): GameDetails? {
        return mapper.load(uuid)?.toGameDetails()
    }

    operator fun get(uuids: Collection<String>): Collection<GameDetails> {
        val query = uuids.map { DynamoGame(uuid = it) }
        return mapper.batchLoad(query).map { it.toGameDetails() }
    }

    @DynamoDBDocument
    data class DynamoGame(
            @DynamoDBHashKey
            var uuid: String? = null,

            var id: String? = null,
            var platform: String? = null,
            var name: String? = null,
            var displayImage: String? = null,
            var icon: String? = null,
            var achievements: List<DynamoAchievement> = emptyList()
    ) {
        constructor(game: Game, achievements: Collection<Achievement>) : this(
                uuid = game.uuid,
                id = game.id,
                platform = game.platform.toString(),
                name = game.name,
                displayImage = game.displayImage,
                icon = game.displayImage,
                achievements = achievements.map { DynamoAchievement(it) }
        )

        fun toGameDetails(): GameDetails {
            val game = Game(
                    id = id!!,
                    platform = Game.Platform.valueOf(platform!!),
                    name = name!!,
                    displayImage = displayImage,
                    icon = icon
            )

            return GameDetails(
                    game = game,
                    achievements = achievements.map { it.toAchievement(game) }
            )
        }
    }

    @DynamoDBDocument
    data class DynamoAchievement(
            var id: String? = null,
            var name: String? = null,
            var description: String? = null,
            var hidden: Int? = null,
            var icons: List<String> = emptyList()
    ) {
        constructor(achievement: Achievement): this(
                id = achievement.id,
                name = achievement.name,
                description = achievement.description,
                hidden = if (achievement.hidden) 1 else 0,
                icons = achievement.icons
        )

        fun toAchievement(game: Game) = Achievement(
                gameId = game.id,
                id = id!!,
                name = name!!,
                description = description,
                hidden = hidden == 1,
                icons = icons
        )
    }
}

