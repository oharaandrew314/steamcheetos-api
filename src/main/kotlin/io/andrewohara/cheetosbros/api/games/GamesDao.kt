package io.andrewohara.cheetosbros.api.games

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.*
import io.andrewohara.cheetosbros.lib.DynamoUtils
import io.andrewohara.cheetosbros.lib.IsoInstantConverter
import io.andrewohara.cheetosbros.lib.UidConverter
import org.mapstruct.InheritInverseConfiguration
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import java.time.Instant

class GamesDao(tableName: String, client: AmazonDynamoDB) {

    val mapper = DynamoUtils.mapper<DynamoGame, Uid, Void>(tableName, client)
    private val dtoMapper = DynamoGameMapperImpl()

    fun save(game: CachedGame) {
        val item = dtoMapper.toDynamo(game)

        mapper.save(item)
    }

    operator fun get(uid: Uid): CachedGame? {
        val item = mapper.load(uid) ?: return null

        return dtoMapper.toModel(item)
    }

    fun batchGet(uids: Collection<Uid>): Collection<CachedGame> {
        val query = uids.map { DynamoGame(uuid = it) }

        return mapper.batchLoad(query)
            .map { dtoMapper.toModel(it) }
    }

    @DynamoDBDocument
    data class DynamoGame(
            @DynamoDBHashKey
            @DynamoDBTypeConverted(converter = UidConverter::class)
            var uuid: Uid? = null,

            var name: String? = null,
            var displayImage: String? = null,

            var achievements: Int = 0,

            @DynamoDBTypeConverted(converter = IsoInstantConverter::class)
            var lastUpdated: Instant = Instant.MIN
    )
}

@Mapper
interface DynamoGameMapper {
    @Mapping(source = "uid", target = "uuid")
    fun toDynamo(game: CachedGame): GamesDao.DynamoGame

    @InheritInverseConfiguration
    fun toModel(game: GamesDao.DynamoGame): CachedGame
}

