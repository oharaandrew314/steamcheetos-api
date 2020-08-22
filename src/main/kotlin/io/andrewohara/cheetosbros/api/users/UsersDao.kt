package io.andrewohara.cheetosbros.api.users

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.*
import io.andrewohara.cheetosbros.lib.DynamoUtils
import io.andrewohara.cheetosbros.sources.Game

class UsersDao(tableName: String, client: AmazonDynamoDB) {

    val mapper = DynamoUtils.mapper<DynamoUser, String, Void>(tableName, client)

    operator fun get(cheetosUserId: String): User? {
        return mapper.load(cheetosUserId)?.toItem()
    }

    operator fun get(platform: Game.Platform, socialUserId: String): User? {
        val query = when (platform) {
            Game.Platform.Xbox -> DynamoDBQueryExpression<DynamoUser>()
                    .withIndexName("xuid")
                    .withHashKeyValues(DynamoUser(xuid = socialUserId))
                    .withConsistentRead(false)
            Game.Platform.Steam -> DynamoDBQueryExpression<DynamoUser>()
                    .withIndexName("steamId64")
                    .withHashKeyValues(DynamoUser(steamId64 = socialUserId))
                    .withConsistentRead(false)
        }

        return mapper.query(query).firstOrNull()?.toItem()
    }

    fun save(user: User) {
        val item = DynamoUser(user)
        mapper.save(item)
    }

    @DynamoDBDocument
    data class DynamoUser(
            @DynamoDBHashKey
            var id: String? = null,
            var displayName: String? = null,

            @DynamoDBIndexHashKey(globalSecondaryIndexName = "xuid")
            var xuid: String? = null,
            var xboxGamertag: String? = null,
            var openxblToken: String? = null,

            @DynamoDBIndexHashKey(globalSecondaryIndexName = "steamId64")
            var steamId64: String? = null,
            var steamUsername: String? = null
    ) {
        fun toItem() = User(
                id = id!!,
                displayName = displayName!!,
                xbox = xuid?.let {
                    SocialLink(
                            id = xuid!!,
                            platform = Game.Platform.Xbox,
                            username = xboxGamertag!!,
                            token = openxblToken!!
                    )
                },
                steam = steamId64?.let {
                    SocialLink(
                            id = steamId64!!,
                            username = steamUsername!!,
                            platform = Game.Platform.Steam,
                            token = null
                    )
                }
        )

        constructor(user: User): this(
                id = user.id,
                displayName = user.displayName,

                xuid = user.xbox?.id,
                xboxGamertag = user.xbox?.username,
                openxblToken = user.xbox?.token,

                steamId64 = user.steam?.id,
                steamUsername = user.steam?.username
        )
    }


}
