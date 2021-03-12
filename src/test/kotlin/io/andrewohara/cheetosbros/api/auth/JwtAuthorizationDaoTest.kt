package io.andrewohara.cheetosbros.api.auth

import io.andrewohara.cheetosbros.TestDriver
import io.andrewohara.cheetosbros.api.users.User
import org.assertj.core.api.Assertions.*
import org.junit.Rule
import org.junit.Test
import java.util.*

class JwtAuthorizationDaoTest {

    @Rule @JvmField val driver = TestDriver

    @Test
    fun `resolve valid token`() {
        val user = User(id = UUID.randomUUID(), emptyMap())

        val token = driver.authorizationDao.assignToken(user)

        assertThat(driver.authorizationDao.resolveUserId(token)).isEqualTo(user.id)
    }

    @Test
    fun `resolve malformed token`() {
        assertThat(driver.authorizationDao.resolveUserId("foo")).isNull()
    }
}