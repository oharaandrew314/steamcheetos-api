package io.andrewohara.cheetosbros.api.games

import io.andrewohara.cheetosbros.api.ApiTestDriver
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.sources.*
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class GamesManagerTest {

    @Rule @JvmField val driver = ApiTestDriver

    private lateinit var testObj: GamesManager

    private lateinit var me3: CachedGame
    private lateinit var me3Achievements: Array<Achievement>
    private lateinit var satisfactory: CachedGame
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

        me3 = driver.createGame(Platform.Xbox, 3,"Mass Effect 3")
        me3Achievements = arrayOf(
                driver.createAchievement(me3, name = "It's a blue alien babe!", description = "Recruit Liara"),
                driver.createAchievement(me3, name = "The Dinosaurs are extinct", description = "Headbutt a Krogan"),
                driver.createAchievement(me3, name = "Did we win?", description = "Choose the colour of your ending")
        )

        satisfactory = driver.createGame(Platform.Steam, 3, "Satisfactory")
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
        val owned1 = driver.addToLibrary(xboxPLayer, me3, 3)
        val owned2 = driver.addToLibrary(steamPlayer, satisfactory, 3)
        assertThat(testObj.listGames(user)).containsExactlyInAnyOrder(owned1, owned2)
    }

    // list achievements

    @Test
    fun `list achievements for missing game`() {
        assertThat(testObj.listAchievements(user, Platform.Steam, "missingGame")).isNull()
    }

    @Test
    fun `list achievements`() {
        driver.addToLibrary(xboxPLayer, me3, 3)
        val progress1 = driver.unlockAchievement(xboxPLayer, me3, me3Achievements[0], Instant.ofEpochSecond(9001))
        val progress2 = driver.unlockAchievement(xboxPLayer, me3, me3Achievements[1], Instant.ofEpochSecond(50000))

        assertThat(testObj.listAchievements(user, me3.uid.platform, me3.uid.id)).containsExactlyInAnyOrder(
            progress1,
            progress2,
            AchievementDetails.create(me3Achievements[2], null)
        )
    }

    // get game

    @Test
    fun `get missing game`() {
        assertThat(testObj.getGame(steamPlayer, "missingGame")).isNull()
    }

    @Test
    fun `get game from wrong platform`() {
        driver.addToLibrary(steamPlayer, satisfactory, 3)
        assertThat(testObj.getGame(xboxPLayer, satisfactory.uid.id)).isNull()
    }

    @Test
    fun `get game`() {
        val owned = driver.addToLibrary(steamPlayer, satisfactory, 3)
        assertThat(testObj.getGame(steamPlayer, satisfactory.uid.id)).isEqualTo(owned)
    }
}