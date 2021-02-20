package io.andrewohara.cheetosbros.api

import spark.Filter
import spark.Request
import spark.Response
import spark.Route

class SparkCorsFilter(private val allowedOrigin: String): Filter, Route {

    override fun handle(request: Request, response: Response) {
        response.header("Access-Control-Allow-Methods", "*")
        response.header("Access-Control-Allow-Origin", allowedOrigin)
        response.header("Access-Control-Allow-Headers", "Authorization")
        response.header("Access-Control-Allow-Credentials", "true")
    }
}