package io.andrewohara.cheetosbros.api.v1

import spark.Spark.*

class BaseApiV1 {
    init {
        options("/*") { _, _ -> "OK" }  // cors
        get("/health") { _, _ -> "OK" }  //health
    }
}