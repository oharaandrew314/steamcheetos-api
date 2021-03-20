package io.andrewohara.cheetosbros.api

import org.http4k.core.*
import org.http4k.lens.RequestContextLens

object AuthFilter {

    operator fun <T> invoke(key: RequestContextLens<T>, lookup: (String) -> T?) = Filter { next ->
        {
            val token = it.bearerToken()
            val user = token?.let(lookup)
            if (user != null) {
                next(it.with(key of user))
            } else {
                next(it)
            }
        }
    }

    private fun Request.bearerToken(): String? = header("Authorization")
        ?.trim()
        ?.takeIf { it.startsWith("Bearer") }
        ?.substringAfter("Bearer")
        ?.trim()
}