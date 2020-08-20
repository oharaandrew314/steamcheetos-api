package io.andrewohara.cheetosbros.auth

import io.andrewohara.cheetosbros.api.auth.JwtAuthorizationDao
import io.andrewohara.cheetosbros.api.auth.PemUtils
import io.andrewohara.cheetosbros.api.users.User
import org.assertj.core.api.Assertions.*
import org.junit.Test

class JwtAuthorizationDaoTest {

    private val testObj = JwtAuthorizationDao(
            issuer = "cheetosbros-test",
            privateKey = PemUtils.parsePEMFile(javaClass.classLoader.getResource("auth/cheetosbros-test.pem")!!)!!,
            publicKey = PemUtils.parsePEMFile(javaClass.classLoader.getResource("auth/cheetosbros-test-pub.pem")!!)!!
    )

    @Test
    fun `resolve valid token`() {
        val user = User(
                id = "1337",
                displayName = "leet user",
                steam = null,
                xbox = null
        )

        val token = testObj.assignToken(user)

        assertThat(testObj.resolveUserId(token)).isEqualTo("1337")
    }

    @Test
    fun `resolve malformed token`() {
        assertThat(testObj.resolveUserId("foo")).isNull()
    }
}