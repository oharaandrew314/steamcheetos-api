package io.andrewohara.cheetosbros.api.v1

import io.andrewohara.cheetosbros.TestDriver
import org.assertj.core.api.Assertions.assertThat
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.junit.Rule
import org.junit.Test

class BaseApiV1Test {

    @Rule @JvmField val driver = TestDriver

    @Test
    fun `can get health`() {
        val response = driver.app(Request(Method.GET, "/health"))
        assertThat(response.status).isEqualTo(Status.OK)
        assertThat(response.bodyString()).isEqualTo("OK")
    }
}