package io.andrewohara.cheetosbros.games

import io.andrewohara.cheetosbros.TestDriver
import io.andrewohara.cheetosbros.api.games.v1.GamesManager
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.sources.*
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class GamesManagerTest {

    @Rule @JvmField val driver = TestDriver()

    private val helper = driver.gamesHelper

    private lateinit var testObj: GamesManager

    private lateinit var me3: Game
    private lateinit var me3Achievements: Array<Achievement>
    private lateinit var satisfactory: Game
    private lateinit var satisfactoryAchievements: Array<Achievement>

    private lateinit var steamPlayer: Player
    private lateinit var xboxPLayer: Player
    private lateinit var user: User

    @Before
    fun setup() {
        testObj = driver.gamesManager

        steamPlayer = helper.createPlayer(Platform.Steam)
        xboxPLayer = helper.createPlayer(Platform.Xbox)

        me3 = helper.createGame(Platform.Xbox, "Mass Effect 3")
        helper.addToLibrary(xboxPLayer, me3)
        me3Achievements = arrayOf(
            helper.createAchievement(me3, name = "It's a blue alien babe!", description = "Recruit Liara"),
            helper.createAchievement(me3, name = "The Dinosaurs are extinct", description = "Headbutt a Krogan"),
            helper.createAchievement(me3, name = "Did we win?", description = "Choose the colour of your ending")
        )

        satisfactory = helper.createGame(platform = Platform.Steam, name = "Satisfactory")
        helper.addToLibrary(steamPlayer, satisfactory)
        satisfactoryAchievements = arrayOf(
            helper.createAchievement(satisfactory, name = "Choo!", description = "Build a train locomotive"),
            helper.createAchievement(satisfactory, name = "3.6 Roentgen", description = "Detonate a nobelisk on a nuclear power plant"),
            helper.createAchievement(satisfactory, name = "You are feeling very sleepy", description = "Collect a Mercer Sphere")
        )

        user = helper.createUser(displayName = "xxNoobSlayerxx", steamPlayer, xboxPLayer)
    }

    // list games

    @Test
    fun `list games for missing user`() {
        val user = User(id = "missingId", displayName = "missingUser")
        assertThat(testObj.listGames(user)).isEmpty()
    }

    @Test
    fun `list games`() {
        assertThat(testObj.listGames(user)).containsExactlyInAnyOrder(me3, satisfactory)
    }

    // list achievements

    @Test
    fun `list achievements for missing game`() {
        assertThat(testObj.listAchievements(user, Platform.Steam, "missingGame")).isNull()
    }

    @Test
    fun `list achievements`() {
        assertThat(testObj.listAchievements(user, me3.platform, me3.id)).containsExactly(*me3Achievements)
    }

    // list achievement status

    @Test
    fun `list achievement status for missing player`() {
        assertThat(testObj.listAchievementStatus(user, playerId = "missingPlayer", gameId = satisfactory.id)).isNull()
    }

    @Test
    fun `list your achievement status`() {
        helper.unlock(steamPlayer, satisfactory, satisfactoryAchievements[0], Instant.ofEpochSecond(9001))
        helper.unlock(steamPlayer, satisfactory, satisfactoryAchievements[1], Instant.ofEpochSecond(50000))

        val expected = arrayOf(
                AchievementStatus(achievementId = satisfactoryAchievements[0].id, unlockedOn = Instant.ofEpochSecond(9001)),
                AchievementStatus(achievementId = satisfactoryAchievements[1].id, unlockedOn = Instant.ofEpochSecond(50000)),
                AchievementStatus(achievementId = satisfactoryAchievements[2].id, unlockedOn = null)
        )

        assertThat(testObj.listAchievementStatus(user, playerId = steamPlayer.id, gameId = satisfactory.id)).containsExactlyInAnyOrder(*expected)
    }

    @Test
    fun `list achievement status for friend`() {
        val friend = helper.createPlayer(Platform.Steam)
        helper.addFriend(steamPlayer, friend)

        helper.unlock(friend, satisfactory, satisfactoryAchievements[0], Instant.ofEpochSecond(9001))
        helper.unlock(friend, satisfactory, satisfactoryAchievements[2], Instant.ofEpochSecond(50000))

        val expected = arrayOf(
                AchievementStatus(achievementId = satisfactoryAchievements[0].id, unlockedOn = Instant.ofEpochSecond(9001)),
                AchievementStatus(achievementId = satisfactoryAchievements[1].id, unlockedOn = null),
                AchievementStatus(achievementId = satisfactoryAchievements[2].id, unlockedOn = Instant.ofEpochSecond(50000))
        )

        assertThat(testObj.listAchievementStatus(user, playerId = friend.id, gameId = satisfactory.id)).containsExactlyInAnyOrder(*expected)
    }

    @Test
    fun `list achievement status for non-friend`() {
        val rando = helper.createPlayer(Platform.Steam)
        helper.addToLibrary(rando, satisfactory)

        helper.unlock(rando, satisfactory, satisfactoryAchievements[0], Instant.ofEpochSecond(9001))
        helper.unlock(rando, satisfactory, satisfactoryAchievements[2], Instant.ofEpochSecond(50000))

        assertThat(testObj.listAchievementStatus(user, playerId = rando.id, gameId = satisfactory.id)).isNull()
    }

    // get game

    @Test
    fun `get missing game`() {
        assertThat(testObj.getGame(Platform.Steam, "missingGame")).isNull()
    }

    @Test
    fun `get game from wrong platform`() {
        assertThat(testObj.getGame(Platform.Steam, me3.id)).isNull()
    }

    @Test
    fun `get game`() {
        assertThat(testObj.getGame(me3.platform, me3.id)).isEqualTo(me3)
    }
}