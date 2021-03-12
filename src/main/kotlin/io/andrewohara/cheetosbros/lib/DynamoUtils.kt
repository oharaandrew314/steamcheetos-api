package io.andrewohara.cheetosbros.lib

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTableMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter
import io.andrewohara.cheetosbros.api.games.Uid
import io.andrewohara.cheetosbros.sources.Platform
import java.time.Instant
import java.util.*

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

class IsoInstantConverter: DynamoDBTypeConverter<String, Instant> {
    override fun convert(instance: Instant)= instance.toString()
    override fun unconvert(serialized: String): Instant = Instant.parse(serialized)
}

class PlatformConverter: DynamoDBTypeConverter<String, Platform> {
    override fun convert(instance: Platform) = instance.toString()
    override fun unconvert(serialized: String): Platform = Platform.valueOf(serialized)
}

class UidConverter: DynamoDBTypeConverter<String, Uid> {
    override fun convert(uid: Uid) = uid.toString()
    override fun unconvert(serialized: String): Uid {
        val (platform, id) = serialized.split("-", limit = 2)
        return Uid(Platform.valueOf(platform), id)
    }
}

class UUIDConverter: DynamoDBTypeConverter<String, UUID> {
    override fun convert(uuid: UUID) = uuid.toString()
    override fun unconvert(serialized: String): UUID = UUID.fromString(serialized)
}