package io.andrewohara.cheetosbros.sources

import org.junit.Before
import org.junit.Test

class SteamDevelopmentHelper {

    private val source = SteamSource(System.getenv("STEAM_API_KEY"))
    private val witcher3 = "292030"

    private val username = "zalpha3146"
    private lateinit var steamId: String

    @Before
    fun setup() {
        steamId = source.resolveUserId(username)!!
    }

    @Test
    fun games() {
        for (game in source.games(steamId)) {
            println(game)
        }
    }

    @Test
    fun `game achievements`() {
        for (achievement in source.achievements(witcher3)) {
            println(achievement)
        }
    }

    @Test
    fun `my achievements`() {
        for (achievement in source.userAchievements(witcher3, steamId)) {
            println(achievement)
        }
    }
}