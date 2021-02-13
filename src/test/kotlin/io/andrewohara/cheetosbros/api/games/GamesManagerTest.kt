package io.andrewohara.cheetosbros.api.games

import io.andrewohara.cheetosbros.api.ApiTestDriver
import io.andrewohara.cheetosbros.api.games.v1.GamesManager
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.sources.*
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class GamesManagerTest {

    @Rule @JvmField val driver = ApiTestDriver()

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

        steamPlayer = driver.createPlayer(Platform.Steam)
        xboxPLayer = driver.createPlayer(Platform.Xbox)

        me3 = driver.createGame(Platform.Xbox, "Mass Effect 3")
        driver.addToLibrary(xboxPLayer, me3)
        me3Achievements = arrayOf(
                driver.createAchievement(me3, name = "It's a blue alien babe!", description = "Recruit Liara"),
                driver.createAchievement(me3, name = "The Dinosaurs are extinct", description = "Headbutt a Krogan"),
                driver.createAchievement(me3, name = "Did we win?", description = "Choose the colour of your ending")
        )

        satisfactory = driver.createGame(platform = Platform.Steam, name = "Satisfactory")
        driver.addToLibrary(steamPlayer, satisfactory)
        satisfactoryAchievements = arrayOf(
                driver.createAchievement(satisfactory, name = "Choo!", description = "Build a train locomotive"),
                driver.createAchievement(satisfactory, name = "3.6 Roentgen", description = "Detonate a nobelisk on a nuclear power plant"),
                driver.createAchievement(satisfactory, name = "You are feeling very sleepy", description = "Collect a Mercer Sphere")
        )

        user = driver.createUser(displayName = "xxNoobSlayerxx", steamPlayer, xboxPLayer)
    }

    // list games

    @Test
    fun `list games for missing user`() {
        val user = User(id = "missingId", displayName = "missingUser")
        assertThat(testObj.listGames(user, Platform.Steam)).isEmpty()
    }

    @Test
    fun `list games`() {
        assertThat(testObj.listGames(user, Platform.Steam)).containsExactlyInAnyOrder(satisfactory)
    }

    // list achievements

    @Test
    fun `list achievements for missing game`() {
        assertThat(testObj.listAchievements(Platform.Steam, "missingGame")).isNull()
    }

    @Test
    fun `list achievements`() {
        assertThat(testObj.listAchievements(me3.platform, me3.id)).containsExactlyInAnyOrder(*me3Achievements)
    }

    // list achievement status

    @Test
    fun `list your achievement status`() {
        driver.unlockAchievement(steamPlayer, satisfactory, satisfactoryAchievements[0], Instant.ofEpochSecond(9001))
        driver.unlockAchievement(steamPlayer, satisfactory, satisfactoryAchievements[1], Instant.ofEpochSecond(50000))

        val expected = arrayOf(
                AchievementStatus(achievementId = satisfactoryAchievements[0].id, unlockedOn = Instant.ofEpochSecond(9001)),
                AchievementStatus(achievementId = satisfactoryAchievements[1].id, unlockedOn = Instant.ofEpochSecond(50000))
        )

        assertThat(testObj.listAchievementStatus(user, satisfactory.platform, satisfactory.id)).containsExactlyInAnyOrder(*expected)
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