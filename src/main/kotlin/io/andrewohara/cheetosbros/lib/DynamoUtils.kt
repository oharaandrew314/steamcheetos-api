package io.andrewohara.cheetosbros.lib

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTableMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter
import io.andrewohara.cheetosbros.sources.Platform
import java.time.Instant

object DynamoUtils {

    inline fun <reified T, H, S> mapper(tableName: String, client: AmazonDynamoDB): DynamoDBTableMapper<T, H, S> {
        val config = DynamoDBMapperConfig.Builder()
                .withTableNameOverride(DynamoDBMapperConfig.TableNameOverride.withTableNameReplacement(tableName))
                .build()

        return DynamoDBMapper(client, config).newTableMapper(T::class.java)
    }
}

class EpochInstantConverter: DynamoDBTypeConverter<Long, Instant> {
    override fun convert(instance: Instant)= instance.epochSecond
    override fun unconvert(serialized: Long): Instant = Instant.ofEpochSecond(serialized)
}

class PlatformConverter: DynamoDBTypeConverter<String, Platform> {
    override fun convert(instance: Platform) = instance.toString()
    override fun unconvert(serialized: String): Platform = Platform.valueOf(serialized)
}