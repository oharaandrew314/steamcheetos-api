package io.andrewohara.cheetosbros.api.users

import java.util.*

typealias UserId = String
typealias Token = String
typealias SteamId64 = Long

interface UsersManager {

    fun createUser(steamId64: SteamId64): User
    fun assignToken(userId: UserId): Token

    fun getUserBySteamId64(steamId64: SteamId64): User?
    fun getUserByToken(token: Token): User?
}

class InMemoryUsersManager: UsersManager {

    private val users = mutableSetOf<User>()
    private val tokens = mutableMapOf<Token, UserId>()

    override fun createUser(steamId64: Long): User {
        val user = User(
                id = UUID.randomUUID().toString(),
                steamId64 = steamId64
        )
        users.add(user)
        return user
    }

    override fun getUserBySteamId64(steamId64: Long): User? {
        return users.firstOrNull { it.steamId64 == steamId64 }
    }

    override fun getUserByToken(token: String): User? {
        val userId = tokens[token] ?: return null
        return users.firstOrNull { it.id == userId }
    }

    override fun assignToken(userId: UserId): Token {
        val token = UUID.randomUUID().toString()
        tokens[token] = userId
        return token
    }
}