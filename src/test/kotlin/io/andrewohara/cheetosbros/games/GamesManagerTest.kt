package io.andrewohara.cheetosbros.games

import io.andrewohara.cheetosbros.TestDriver
import io.andrewohara.cheetosbros.api.games.v1.GamesManager
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.sources.Achievement
import io.andrewohara.cheetosbros.sources.AchievementStatus
import io.andrewohara.cheetosbros.sources.Game
import io.andrewohara.cheetosbros.sources.Player
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class GamesManagerTest {

    @Rule @JvmField val driver = TestDriver()

    private lateinit var testObj: GamesManager

    private lateinit var me3: Game
    private lateinit var me3Achievements: List<Achievement>
    private lateinit var satisfactory: Game
    private lateinit var satisfactoryAchievements: List<Achievement>

    private lateinit var player: Player
    private lateinit var user: User

    @Before
    fun setup() {
        testObj = driver.gamesManager

        player = driver.createPlayer()

        me3 = driver.createGame(name = "Mass Effect 3")
        driver.addToLibrary(me3, player)
        me3Achievements = listOf(
            driver.createAchievement(me3, name = "It's a blue alien babe!", description = "Recruit Liara"),
            driver.createAchievement(me3, name = "The Dinosaurs are extinct", description = "Headbutt a Krogan"),
            driver.createAchievement(me3, name = "Did we win?", description = "Choose the colour of your ending")
        )

        satisfactory = driver.createGame(name = "Satisfactory")
        driver.addToLibrary(satisfactory, player)
        satisfactoryAchievements = listOf(
            driver.createAchievement(satisfactory, name = "Choo!", description = "Build a train locomotive"),
            driver.createAchievement(satisfactory, name = "3.6 Roentgen", description = "Detonate a nobelisk on a nuclear power plant"),
            driver.createAchievement(satisfactory, name = "You are feeling very sleepy", description = "Collect a Mercer Sphere")
        )

        user = driver.createUser(displayName = "xxNoobSlayerxx", player)

        driver.sync(user)
    }

    @Test
    fun `list games for missing user`() {
        val user = User(id = "missingId", displayName = "missingUser")
        assertThat(testObj.listGames(user)).isEmpty()
    }

    @Test
    fun `list games`() {
        assertThat(testObj.listGames(user)).containsExactlyInAnyOrder(me3, satisfactory)
    }

    @Test
    fun `list achievements for missing user`() {
        val expected = satisfactoryAchievements.map {
            GamesManager.AchievementDetails(
                    achievement = it,
                    status = AchievementStatus(achievementId = it.id, unlockedOn = null)
            )
        }

        val user = User(id = "missingId", displayName = "missingUser")

        // may want to change result to be null instead, but takes extra db calls to do so
        assertThat(testObj.listAchievements(user, satisfactory.uuid)?.toSet()).isEqualTo(expected.toSet())
    }

    @Test
    fun `list achievements for missing game`() {
        assertThat(testObj.listAchievements(user, "missingGame")).isNull()
    }

    @Test
    fun `list un-achieved achievements`() {
        val expected = me3Achievements.map {
            GamesManager.AchievementDetails(
                    achievement = it,
                    status = AchievementStatus(achievementId = it.id, unlockedOn = null)
            )
        }

        assertThat(testObj.listAchievements(user, me3.uuid)?.toSet()).isEqualTo(expected.toSet())
    }

    @Test
    fun `list achieved achievements`() {
        driver.unlockAchievement(player, satisfactory, satisfactoryAchievements[0], Instant.ofEpochSecond(9001))
        driver.unlockAchievement(player, satisfactory, satisfactoryAchievements[1], Instant.ofEpochSecond(50000))
        driver.sync(user)

        val expected = setOf(
                GamesManager.AchievementDetails(
                        achievement = satisfactoryAchievements[0],
                        status = AchievementStatus(achievementId = satisfactoryAchievements[0].id, unlockedOn = Instant.ofEpochSecond(9001))
                ),
                GamesManager.AchievementDetails(
                        achievement = satisfactoryAchievements[1],
                        status = AchievementStatus(achievementId = satisfactoryAchievements[1].id, unlockedOn = Instant.ofEpochSecond(50000))
                ),
                GamesManager.AchievementDetails(
                        achievement = satisfactoryAchievements[2],
                        status = AchievementStatus(achievementId = satisfactoryAchievements[2].id, unlockedOn = null)
                )
        )

        assertThat(testObj.listAchievements(user, satisfactory.uuid)).containsExactlyInAnyOrder(*expected.toTypedArray())
    }

    @Test
    fun `get missing game`() {
        assertThat(testObj.getGame("missingGame")).isNull()
    }

    @Test
    fun `get game you don't own`() {
        val callOfDuty100 = driver.createGame(name = "Call of Duty 100 - we're not even trying anymore")
        val otherPlayer = driver.createPlayer()
        val otherUser = driver.createUser(displayName = "otherUser", otherPlayer)
        driver.addToLibrary(callOfDuty100, otherPlayer)
        driver.sync(otherUser)

        assertThat(testObj.getGame(callOfDuty100.uuid)).isEqualTo(callOfDuty100)
    }

    @Test
    fun `get game`() {
        assertThat(testObj.getGame(me3.uuid)).isEqualTo(me3)
    }
}