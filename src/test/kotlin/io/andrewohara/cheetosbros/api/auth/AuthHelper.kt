package io.andrewohara.cheetosbros.api.auth

import org.openid4java.consumer.ConsumerManager

fun main() {
    val consumer = ConsumerManager().apply {
        maxAssocAttempts = 0
    }

    val discoveries = consumer.discover("http://steamcommunity.com/openid")
    val info = requireNotNull(consumer.associate(discoveries))
    println(info)

    val authUri = consumer.authenticate(info, "http://localhost:3000/callback")
    println(authUri.getDestinationUrl(true))
}