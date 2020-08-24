package io.andrewohara.cheetosbros.api.users

data class User(
        val id: String,
        val displayName: String,
        val openxblToken: String? = null
)