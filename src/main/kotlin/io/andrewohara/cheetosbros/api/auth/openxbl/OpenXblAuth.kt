package io.andrewohara.cheetosbros.api.auth.openxbl

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.andrewohara.cheetosbros.api.users.SocialLink
import io.andrewohara.cheetosbros.sources.Game
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class OpenXblAuth(private val publicAppKey: String) {

    private val client = HttpClient.newHttpClient()
    private val mapper = jacksonObjectMapper()

    fun getLoginUrl() = "https://xbl.io/app/auth/$publicAppKey"

    fun verify(code: String): SocialLink {
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

        val result = mapper.readValue(response.body(), OpenXblAuthResult::class.java)

        return SocialLink(
                platform = Game.Platform.Xbox,
                id = result.xuid,
                username = result.gamertag,
                token = result.app_key
        )
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenXblAuthResult(
        val xuid: String,
        val gamertag: String,
        val avatar: String,
        val gamerscore: String,
        val app_key: String
)