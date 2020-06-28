package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.sources.openxbl.OpenXblSource
import org.junit.Before
import org.junit.Test

class OpenXblDevelopmentHelper {

    private val source = OpenXblSource(System.getenv("OPENXBL_API_KEY"))

    private val username = "zalpha5436"
    private lateinit var userId: String
    private val diablo3 = "2117764661"

    @Before
    fun setup() {
        userId = source.resolveUserId(username)!!
    }

    @Test
    fun games() {
        for (game in source.games(userId)) {
            println(game)
        }
    }

    @Test
    fun `game achievements`() {
        for (achievement in source.achievements(diablo3)) {
            println(achievement)
        }
    }

    @Test
    fun `my achievements`() {
        for (achievement in source.userAchievements(diablo3, userId)) {
            println(achievement)
        }
    }
}