package io.andrewohara.cheetosbros.api

import org.http4k.server.SunHttp
import org.http4k.server.asServer
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.readText

object DevelopmentApiServer {

    @ExperimentalPathApi
    @JvmStatic
    fun main(args: Array<String>) {
        val privateKey = Paths.get(System.getenv("PRIVATE_PEM_PATH")).readText()
        val publicKey = Paths.get(System.getenv("PUBLIC_PEM_PATH")).readText()

        val services = ServiceBuilder.fromProps(System.getenv())
        val authDao = services.createJwtAuth(
            issuer = "localhost",
            privateKey = privateKey,
            publicKey = publicKey,
        )

        services.createHttp(authDao)
            .asServer(SunHttp(8000))
            .start()
    }
}