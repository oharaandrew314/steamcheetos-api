package io.andrewohara.cheetosbros.api.auth

import io.andrewohara.cheetosbros.api.ApiTestDriver
import io.andrewohara.cheetosbros.api.users.User
import org.assertj.core.api.Assertions.*
import org.junit.Rule
import org.junit.Test

class JwtAuthorizationDaoTest {

    @Rule @JvmField val driver = ApiTestDriver()

    @Test
    fun `resolve valid token`() {
        val user = User(id = "1337", displayName = "leet user")

        val token = driver.authorizationDao.assignToken(user)

        assertThat(driver.authorizationDao.resolveUserId(token)).isEqualTo("1337")
    }

    @Test
    fun `resolve malformed token`() {
        assertThat(driver.authorizationDao.resolveUserId("foo")).isNull()
    }
}