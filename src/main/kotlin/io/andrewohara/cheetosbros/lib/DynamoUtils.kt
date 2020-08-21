package io.andrewohara.cheetosbros.lib

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTableMapper

object DynamoUtils {

    inline fun <reified T, H, S> mapper(tableName: String, client: AmazonDynamoDB): DynamoDBTableMapper<T, H, S> {
        val config = DynamoDBMapperConfig.Builder()
                .withTableNameOverride(DynamoDBMapperConfig.TableNameOverride.withTableNameReplacement(tableName))
                .build()

        return DynamoDBMapper(client, config).newTableMapper(T::class.java)
    }
}