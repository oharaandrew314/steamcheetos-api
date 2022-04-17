package io.andrewohara.cheetosbros.games

import org.http4k.core.Uri

fun Collection<Game>.withImageHost(host: Uri) = map { it.withImageHost(host) }

fun Game.withImageHost(host: Uri) = copy(
    displayImage = displayImage.withHost(host)
)