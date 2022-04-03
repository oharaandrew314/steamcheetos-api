package io.andrewohara.cheetosbros.games

import io.andrewohara.cheetosbros.TestDriver
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
class GameServiceTest {

    private val driver = TestDriver()

    private val testObj = driver.gameService

    private val userId = "42069"
    private val me3 = driver.createGame(
        userId,
        "Mess Effect 3",
        Triple("It's a blue alien babe!", "Recruit Liara", null),
        Triple("The Dinasaurs are extinct", "Headbutt a Krogan", null),
        Triple("Did we win?" ,"Choose the colour of your ending", null)
    )

    private val satisfactory = driver.createGame(
        userId,
        "Satisfactory",
        Triple("Choo!", "Build a train locomotice", null),
        Triple("3.6 Roentgen", "Detonate a nobelisk on a nuclear power plant", null),
        Triple("You are feeling very sleepy", "Collect a Mercer Sphere", null)
    )

    // list games

    @Test
    fun `list games for missing user`() {
        testObj.listGames("missingUser").shouldBeEmpty()
    }

    @Test
    fun `list games`() {
        testObj.listGames(userId).shouldContainExactlyInAnyOrder(
            me3.game, satisfactory.game
        )
    }

    // list achievements

    @Test
    fun `list achievements for missing game`() {
        testObj.listAchievements(userId, "missingGame").shouldBeEmpty()
    }

    @Test
    fun `list achievements`() {
        testObj.listAchievements(userId, me3.game.id).shouldContainExactly(me3.achievements)
        testObj.listAchievements(userId, satisfactory.game.id).shouldContainExactly(satisfactory.achievements)
    }

    // get game

    @Test
    fun `get game for missing user`() {
        testObj.getGame("missingUser", me3.game.id).shouldBeNull()
    }

    @Test
    fun `get missing game`() {
        testObj.getGame(userId, "missingGame").shouldBeNull()
    }

    @Test
    fun `get game`() {
        testObj.getGame(userId, me3.game.id) shouldBe me3.game
    }
}