package io.andrewohara.cheetosbros.api

import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.readText

object DevelopmentApiServer {

    @ExperimentalPathApi
    @JvmStatic
    fun main(args: Array<String>) {
        val privateKey = Paths.get(System.getenv("PRIVATE_PEM_PATH")).readText()
        val publicKey = Paths.get(System.getenv("PUBLIC_PEM_PATH")).readText()
        val steamKey = System.getenv("STEAM_API_KEY")

        val services = ServiceBuilder.fromEnv(steamKey)
        val auth = AuthBuilder.buildJwt(
            publicKeyIssuer = "localhost",
            privateKey = privateKey,
            publicKey = publicKey,
            usersDao = services.usersDao,
            socialLinkDao = services.socialLinks
        )

        services.startSpark(auth,8000, cors = true, decodeQueryParams = false)
    }
}