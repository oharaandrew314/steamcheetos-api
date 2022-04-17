package io.andrewohara.cheetosbros.games

import org.http4k.core.Uri

fun Collection<Achievement>.witImageHost(host: Uri) = map {
    it.copy(
        iconLocked = it.iconLocked.withHost(host),
        iconUnlocked = it.iconUnlocked.withHost(host)
    )
}

fun Uri.withHost(host: Uri) = scheme(host.scheme).host(host.host).port(host.port)