package io.andrewohara.cheetosbros.games

import dev.forkhandles.result4k.onFailure
import io.andrewohara.cheetosbros.sources.AchievementData
import io.andrewohara.lib.batchPutItem
import org.http4k.connect.amazon.dynamodb.*
import org.http4k.connect.amazon.dynamodb.model.Attribute
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.dynamodb.model.with
import org.http4k.core.Uri
import org.http4k.format.autoDynamoLens
import java.io.IOException
import java.time.Instant

class AchievementsDao(private val dynamoDb: DynamoDb, private val tableName: TableName) {

    private val lens = DynamoDbMoshi.autoDynamoLens<Achievement>()

    private val libraryIdAttr = Attribute.string().map(
        nextIn = { LibraryId.parse(it) },
        nextOut = { it.toString() }
    ).required("libraryId")

    private val achievementIdAttr = Attribute.string().required("id")

    operator fun plusAssign(achievements: Collection<Achievement>) {
        dynamoDb.batchPutItem(tableName, achievements, lens)
            .onFailure { throw IOException("Error putting achievements: $it") }
    }

    operator fun plusAssign(achievement: Achievement) {
        val item = Item().with(lens of achievement)
        dynamoDb.putItem(tableName, item)
            .onFailure { throw IOException("Error putting achievement: $it") }
    }

    operator fun get(userId: String, gameId: String): Collection<Achievement> {
        val libraryId = LibraryId(userId, gameId)

        return dynamoDb.queryPaginated(
            TableName = tableName,
            KeyConditionExpression = "$libraryIdAttr = :val1",
            ExpressionAttributeValues = mapOf(":val1" to libraryIdAttr.asValue(libraryId))
        )
            .flatMap { page -> page.onFailure { throw IOException("Error listing achievements: $it") } }
            .map(lens)
            .toList()
    }

    operator fun get(userId: String, gameId: String, achievementId: String): Achievement? {
        return dynamoDb.getItem(
            TableName = tableName,
            Key = Item(
                libraryIdAttr of LibraryId(userId, gameId),
                achievementIdAttr of achievementId
            ))
            .onFailure { throw IOException("Error getting achievements: $it") }
            .item
            ?.let(lens)
    }
}

data class Achievement(
    val libraryId: String,
    val id: String,

    val name: String,
    val description: String?,
    val hidden: Boolean,
    val unlockedOn: Instant?,

    val iconLocked: Uri,
    val iconUnlocked: Uri,

    val favourite: Boolean = false,
)

fun Achievement.toData() = AchievementData(
    gameId = LibraryId.parse(libraryId).gameId,
    id = id,
    name = name,
    description = description,
    hidden = hidden,
    iconUnlocked = iconUnlocked,
    iconLocked = iconLocked
)

fun AchievementData.toAchievement(userId: String, unlockedOn: Instant?) = Achievement(
    libraryId = LibraryId(userId, gameId).toString(),
    id = id,
    name = name,
    description = description,
    hidden = hidden,
    iconLocked = iconLocked,
    iconUnlocked = iconUnlocked,
    unlockedOn = unlockedOn
)

data class LibraryId(
    val userId: String,
    val gameId: String,
) {
    companion object {
        fun parse(value: String): LibraryId {
            val (userId, gameId) = value.split(":")
            return LibraryId(userId, gameId)
        }
    }

    override fun toString() = "$userId:$gameId"
}