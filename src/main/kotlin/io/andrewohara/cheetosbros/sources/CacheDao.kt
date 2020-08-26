package io.andrewohara.cheetosbros.sources

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted
import io.andrewohara.cheetosbros.lib.DynamoUtils
import io.andrewohara.cheetosbros.lib.EpochInstantConverter
import java.time.Instant

class CacheDao(tableName: String, client: AmazonDynamoDB) {

    val mapper = DynamoUtils.mapper<DynamoCacheItem, String, Void>(tableName, client)

    fun isLibraryCacheExpired(player: Player): Boolean {
        return mapper.load("${player.platform}-${player.id}-library") == null
    }

    fun updateLibraryCache(player: Player, ttl: Instant) {
        val item = DynamoCacheItem("${player.platform}-${player.id}-library", ttl = ttl)
        mapper.save(item)
    }

    fun isAchievementsCacheExpired(game: Game): Boolean {
        return mapper.load("${game.platform}-${game.id}-achievements") == null
    }

    fun updateAchievementsCache(game: Game, ttl: Instant) {
        mapper.save(DynamoCacheItem(uuid = "${game.platform}-${game.id}-achievements", ttl = ttl))
    }

    fun isAchievementStatusCacheExpired(game: Game, player: Player): Boolean {
        return mapper.load("${player.platform}-${player.id}-${game.id}-achievementStatus") == null
    }

    fun updateAchievementStatusCache(game: Game, player: Player, ttl: Instant) {
        mapper.save(DynamoCacheItem(uuid = "${player.platform}-${player.id}-${game.id}-achievementStatus", ttl = ttl))
    }

    fun isFriendsCacheExpired(player: Player): Boolean {
        return mapper.load("${player.platform}-${player.id}-friends") == null
    }

    fun updateFriendsCache(player: Player, ttl: Instant) {
        val item = DynamoCacheItem("${player.platform}-${player.id}-friends", ttl = ttl)
        mapper.save(item)
    }

    @DynamoDBDocument
    data class DynamoCacheItem(
            @DynamoDBHashKey
            var uuid: String? = null,

            @DynamoDBTypeConverted(converter = EpochInstantConverter::class)
            var ttl: Instant? = null
    )
}