package io.andrewohara.cheetosbros.api.v1

import io.andrewohara.cheetosbros.TestDriver
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.sources.Platform
import org.assertj.core.api.Assertions.assertThat
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.junit.Rule
import org.junit.Test

class GamesApiV1Test {

    @Rule @JvmField val driver = TestDriver

    @Test
    fun `list games`() {
        val user = driver.saveUser(steam = true)
        val player = user.players.getValue(Platform.Steam)

        val game = driver.saveGame(Platform.Steam, 3)
        driver.saveToLibrary(player, game, 1)

        val request = Request(Method.GET, "/v1/games")
            .asUser(user)

        val response = driver.app(request)
        assertThat(response.status).isEqualTo(Status.OK)

        assertThat(response.bodyString()).isEqualTo("""[{"uid":{"platform":"Steam","id":"${game.uid.id}"},"name":"${game.name}","achievementsTotal":3,"achievementsCurrent":1,"lastUpdated":"${driver.time}"}]""")
    }

    private fun Request.asUser(user: User): Request {
        return header("Authorization", "Bearer ${driver.authorizationDao.assignToken(user)}")
    }
}