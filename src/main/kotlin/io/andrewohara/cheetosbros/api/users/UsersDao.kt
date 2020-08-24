package io.andrewohara.cheetosbros.api.users

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.*
import io.andrewohara.cheetosbros.lib.DynamoUtils

class UsersDao(tableName: String, client: AmazonDynamoDB) {

    val mapper = DynamoUtils.mapper<DynamoUser, String, Void>(tableName, client)

    operator fun get(cheetosUserId: String): User? {
        return mapper.load(cheetosUserId)?.toUser()
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
            var openxblToken: String? = null
    ) {
        fun toUser() = User(
                id = id!!,
                displayName = displayName!!,
                openxblToken = openxblToken
        )

        constructor(user: User): this(
                id = user.id,
                displayName = user.displayName,
                openxblToken = user.openxblToken
        )
    }
}
