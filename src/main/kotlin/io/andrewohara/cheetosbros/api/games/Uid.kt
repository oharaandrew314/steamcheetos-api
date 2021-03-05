package io.andrewohara.cheetosbros.api.games

import io.andrewohara.cheetosbros.sources.Platform

data class Uid(val platform: Platform, val id: String) {
    override fun toString() = "$platform-$id"
}