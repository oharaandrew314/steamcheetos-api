package io.andrewohara.cheetosbros.api.users

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted
import io.andrewohara.cheetosbros.api.games.Uid
import io.andrewohara.cheetosbros.lib.DynamoUtils
import io.andrewohara.cheetosbros.lib.UidConverter
import io.andrewohara.cheetosbros.sources.Player

class SocialLinkDao(dynamo: AmazonDynamoDB, tableName: String) {

    val mapper = DynamoUtils.mapper<DynamoSocialLink, Uid, Unit>(tableName, dynamo)

    fun link(player: Player, user: User) {
        val item = DynamoSocialLink(player.uid, user.id)
        mapper.save(item)
    }

    fun lookupUserId(player: Player): String? {
        return mapper.load(player.uid)?.userId
    }
}

@DynamoDBDocument
data class DynamoSocialLink(
    @DynamoDBHashKey
    @DynamoDBTypeConverted(converter = UidConverter::class)
    var playerUuid: Uid? = null,

    var userId: String? = null
)