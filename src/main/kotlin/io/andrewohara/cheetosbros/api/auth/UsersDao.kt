package io.andrewohara.cheetosbros.api.auth

import io.andrewohara.dynamokt.DynamoKtPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.Key
import java.util.*

//class UsersDao(private val table: DynamoDbTable<User>) {
//
//    operator fun get(uuid: String): User? {
//        val key = Key.builder().partitionValue(uuid).build()
//        return table.getItem(key)
//    }
//
//    operator fun plusAssign(user: User) {
//        table.putItem(user)
//    }
//}
//
//data class User(
//    @DynamoKtPartitionKey
//    val id: String,
//
//    val username: String,
//    val avatar: String?
//)