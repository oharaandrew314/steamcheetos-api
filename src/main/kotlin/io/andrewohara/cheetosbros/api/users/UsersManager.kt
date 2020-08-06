package io.andrewohara.cheetosbros.api.users

import io.andrewohara.cheetosbros.sources.steam.SteamSource
import java.util.*

typealias UserId = String
typealias Token = String
typealias SteamId64 = Long
typealias XboxUsername = String

interface UsersManager {

    fun createUser(steamId64: SteamId64): User
    fun createUser(xboxUsername: XboxUsername, xboxToken: String): User
    fun assignToken(userId: UserId): Token

    fun getUserByXboxUsername(username: XboxUsername): User?
    fun getUserBySteamId64(steamId64: SteamId64): User?
    fun getUserByToken(token: Token): User?
}

class InMemoryUsersManager(private val steamSource: SteamSource): UsersManager {

    private val users = mutableSetOf<User>()
    private val tokens = mutableMapOf<Token, UserId>()

    override fun createUser(xboxUsername: XboxUsername, xboxToken: String): User {

        val user = User(
                id = UUID.randomUUID().toString(),
                displayName = xboxUsername,
                steamId64 = null,
                xboxUsername = xboxUsername,
                xboxToken = xboxToken
        )
        users.add(user)
        return user
    }

    override fun createUser(steamId64: SteamId64): User {
        val player = steamSource.getPlayer(steamId64.toString())
        val username = player?.displayName ?: "steam$steamId64"

        val user = User(
                id = UUID.randomUUID().toString(),
                displayName = username,
                steamId64 = steamId64,
                xboxUsername = null,
                xboxToken = null
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