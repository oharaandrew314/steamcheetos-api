package io.andrewohara.cheetosbros.api.users

import java.lang.IllegalArgumentException
import java.util.*

typealias UserId = String
typealias Token = String
typealias SteamId64 = Long
typealias XboxUsername = String

interface UsersManager {

    fun createUser(steamId64: SteamId64? = null, xboxUsername: XboxUsername? = null): User
    fun assignToken(userId: UserId): Token

    fun getUserByXboxUsername(username: XboxUsername): User?
    fun getUserBySteamId64(steamId64: SteamId64): User?
    fun getUserByToken(token: Token): User?
}

class InMemoryUsersManager: UsersManager {

    private val users = mutableSetOf<User>()
    private val tokens = mutableMapOf<Token, UserId>()

    override fun createUser(steamId64: Long?, xboxUsername: XboxUsername?): User {
        if (steamId64 == null && xboxUsername == null) throw IllegalArgumentException("steam and xbox cannot have null identities")

        val user = User(
                id = UUID.randomUUID().toString(),
                steamId64 = steamId64,
                xboxUsername = xboxUsername
        )
        users.add(user)
        return user
    }

    override fun getUserBySteamId64(steamId64: Long): User? {
        return users.firstOrNull { it.steamId64 == steamId64 }
    }

    override fun getUserByXboxUsername(username: XboxUsername): User? {
        return users.firstOrNull { it.xboxUsername == username }
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