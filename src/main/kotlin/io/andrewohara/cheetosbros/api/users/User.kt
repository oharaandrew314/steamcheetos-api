package io.andrewohara.cheetosbros.api.users

data class User(
        val id: String,
        val displayName: String,
        val steamId64: Long?,
        val xboxUsername: String?,
        val xboxToken: String?
)