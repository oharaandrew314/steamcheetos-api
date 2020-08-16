package io.andrewohara.cheetosbros.api.users

import io.andrewohara.cheetosbros.sources.steam.SteamSource
import java.util.*

typealias UserId = String
typealias Token = String
typealias SteamId64 = Long

interface UsersManager {

    fun createUser(steamId64: SteamId64): User
    fun createUser(xuid: String, gamertag: String, openXblToken: String): User

    fun assignToken(userId: UserId): Token

    fun getUserByXuid(xuid: String): User?
    fun getUserBySteamId64(steamId64: SteamId64): User?
    fun getUserByToken(token: Token): User?
}

class InMemoryUsersManager(private val steamSource: SteamSource): UsersManager {

    private val users = mutableSetOf<User>()
    private val tokens = mutableMapOf<Token, UserId>()

    override fun createUser(xuid: String, gamertag: String, openXblToken: String): User {
        val user = User(
                id = UUID.randomUUID().toString(),
                displayName = gamertag,
                steam = null,
                xbox = XboxUser(
                        xuid = xuid,
                        gamertag = gamertag,
                        token = openXblToken
                )
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
                steam = SteamUser(
                        steamId64 = steamId64,
                        username = username
                ),
                xbox = null
        )
        users.add(user)
        return user
    }

    override fun getUserBySteamId64(steamId64: Long): User? {
        return users.firstOrNull { it.steam?.steamId64 == steamId64 }
    }

//    fun getUserByXboxGamertag(username: XboxUsername): User? {
//        return users.firstOrNull { it.xbox?.gamertag == username }
//    }

    override fun getUserByXuid(xuid: String): User? {
        return users.firstOrNull { it.xbox?.xuid == xuid }
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