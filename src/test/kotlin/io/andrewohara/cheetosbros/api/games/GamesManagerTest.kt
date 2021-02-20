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
        user = driver.createUser(xbox = xboxPLayer, steam = steamPlayer)

        me3 = driver.createGame(Platform.Xbox, "Mass Effect 3")
        me3Achievements = arrayOf(
                driver.createAchievement(me3, name = "It's a blue alien babe!", description = "Recruit Liara"),
                driver.createAchievement(me3, name = "The Dinosaurs are extinct", description = "Headbutt a Krogan"),
                driver.createAchievement(me3, name = "Did we win?", description = "Choose the colour of your ending")
        )

        satisfactory = driver.createGame(platform = Platform.Steam, name = "Satisfactory")
        satisfactoryAchievements = arrayOf(
                driver.createAchievement(satisfactory, name = "Choo!", description = "Build a train locomotive"),
                driver.createAchievement(satisfactory, name = "3.6 Roentgen", description = "Detonate a nobelisk on a nuclear power plant"),
                driver.createAchievement(satisfactory, name = "You are feeling very sleepy", description = "Collect a Mercer Sphere")
        )
    }

    // list games

    @Test
    fun `list games for missing user`() {
        val user = User(id = "missingId", players = emptyMap())
        assertThat(testObj.listGames(user)).isEmpty()
    }

    @Test
    fun `list games`() {
        val owned1 = driver.addToLibrary(xboxPLayer, me3, 0, 3)
        val owned2 = driver.addToLibrary(steamPlayer, satisfactory, 0, 3)
        assertThat(testObj.listGames(user)).containsExactlyInAnyOrder(owned1, owned2)
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
        driver.addToLibrary(xboxPLayer, me3, 0, 3)
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
        assertThat(testObj.getGame(steamPlayer, "missingGame")).isNull()
    }

    @Test
    fun `get game from wrong platform`() {
        driver.addToLibrary(steamPlayer, satisfactory, 0, 3)
        assertThat(testObj.getGame(xboxPLayer, satisfactory.id)).isNull()
    }

    @Test
    fun `get game`() {
        val owned = driver.addToLibrary(steamPlayer, satisfactory, 0, 3)
        assertThat(testObj.getGame(steamPlayer, satisfactory.id)).isEqualTo(owned)
    }
}