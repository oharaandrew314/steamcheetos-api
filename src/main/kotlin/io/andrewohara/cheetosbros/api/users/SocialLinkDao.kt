package io.andrewohara.cheetosbros.api.users

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import io.andrewohara.cheetosbros.lib.DynamoUtils
import io.andrewohara.cheetosbros.sources.Player

interface SocialLinkDao {
    fun link(player: Player, user: User)
    fun lookupUserId(player: Player): String?
}

class DynamoSocialLinkDao(dynamo: AmazonDynamoDB, tableName: String): SocialLinkDao {

    val mapper = DynamoUtils.mapper<DynamoSocialLink, String, Unit>(tableName, dynamo)

    override fun link(player: Player, user: User) {
        val item = DynamoSocialLink(player.uuid(), user.id)
        mapper.save(item)
    }

    override fun lookupUserId(player: Player): String? {
        return mapper.load(player.uuid())?.userId
    }

    private fun Player.uuid() = "$platform-$id"
}

@DynamoDBDocument
data class DynamoSocialLink(
    @DynamoDBHashKey
    var playerUuid: String? = null,

    var userId: String? = null
)