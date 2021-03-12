package io.andrewohara.cheetosbros.api.games

import io.andrewohara.cheetosbros.TestDriver
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.sources.*
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.util.*

class GamesManagerTest {

    @Rule @JvmField val driver = TestDriver

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

        user = driver.saveUser(xbox = true, steam = true)
        steamPlayer = user.players.getValue(Platform.Steam)
        xboxPLayer = user.players.getValue(Platform.Xbox)

        me3 = driver.saveGame(Platform.Xbox, 3,"Mass Effect 3")
        me3Achievements = arrayOf(
                driver.saveAchievement(me3, name = "It's a blue alien babe!", description = "Recruit Liara"),
                driver.saveAchievement(me3, name = "The Dinosaurs are extinct", description = "Headbutt a Krogan"),
                driver.saveAchievement(me3, name = "Did we win?", description = "Choose the colour of your ending")
        )

        satisfactory = driver.saveGame(Platform.Steam, 3, "Satisfactory")
        satisfactoryAchievements = arrayOf(
                driver.saveAchievement(satisfactory, name = "Choo!", description = "Build a train locomotive"),
                driver.saveAchievement(satisfactory, name = "3.6 Roentgen", description = "Detonate a nobelisk on a nuclear power plant"),
                driver.saveAchievement(satisfactory, name = "You are feeling very sleepy", description = "Collect a Mercer Sphere")
        )
    }

    // list games

    @Test
    fun `list games for missing user`() {
        val user = User(id = UUID.randomUUID(), players = emptyMap())
        assertThat(testObj.listGames(user)).isEmpty()
    }

    @Test
    fun `list games`() {
        val owned1 = driver.saveToLibrary(xboxPLayer, me3, 3)
        val owned2 = driver.saveToLibrary(steamPlayer, satisfactory, 3)
        assertThat(testObj.listGames(user)).containsExactlyInAnyOrder(owned1, owned2)
    }

    // list achievements

    @Test
    fun `list achievements for missing game`() {
        assertThat(testObj.listAchievements(user, Platform.Steam, "missingGame")).isNull()
    }

    @Test
    fun `list achievements`() {
        driver.saveToLibrary(xboxPLayer, me3, 3)
        val progress1 = driver.saveProgress(xboxPLayer, me3, me3Achievements[0], Instant.ofEpochSecond(9001))
        val progress2 = driver.saveProgress(xboxPLayer, me3, me3Achievements[1], Instant.ofEpochSecond(50000))

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
        driver.saveToLibrary(steamPlayer, satisfactory, 3)
        assertThat(testObj.getGame(xboxPLayer, satisfactory.uid.id)).isNull()
    }

    @Test
    fun `get game`() {
        val owned = driver.saveToLibrary(steamPlayer, satisfactory, 3)
        assertThat(testObj.getGame(steamPlayer, satisfactory.uid.id)).isEqualTo(owned)
    }
}