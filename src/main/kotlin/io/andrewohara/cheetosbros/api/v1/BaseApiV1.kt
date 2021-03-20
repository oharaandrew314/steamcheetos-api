package io.andrewohara.cheetosbros.api.v1

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.core.Method.GET

object BaseApiV1 {
//    init {
//        options("/*") { _, _ -> "OK" }  // cors
//        get("/health") { _, _ -> "OK" }  //health
//    }

    fun getRoutes() = routes(
        "/health" bind GET to ::health
    )

    private fun health(request: Request): Response {
        return Response(OK).body("OK")
    }
}