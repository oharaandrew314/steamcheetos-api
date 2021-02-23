package io.andrewohara.cheetosbros.api.auth

import com.squareup.moshi.Moshi
import io.andrewohara.cheetosbros.sources.Platform
import io.andrewohara.cheetosbros.sources.Player
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class OpenXblAuth(private val publicAppKey: String) {

    private val client = HttpClient.newHttpClient()
    private val adapter = Moshi.Builder().build().adapter(OpenXblAuthResult::class.java)

    fun getLoginUrl() = "https://xbl.io/app/auth/$publicAppKey"

    fun verify(code: String): Player {
        val payload = """{"code": "$code", "app_key": "$publicAppKey"}"""

        val request = HttpRequest
                .newBuilder(URI.create("https://xbl.io/app/claim"))
                .header("X-Contract", "2")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            throw IOException("Error authorizing request: ${response.body()}")
        }

        val result = adapter.fromJson(response.body())!!

        return Player(
                platform = Platform.Xbox,
                id = result.xuid,
                username = result.gamertag,
                avatar = result.avatar,
                token = result.app_key
        )
    }

    private data class OpenXblAuthResult(
            val xuid: String,
            val gamertag: String,
            val avatar: String,
            val gamerscore: String,
            val app_key: String
    )
}

