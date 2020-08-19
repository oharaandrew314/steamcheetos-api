package io.andrewohara.cheetosbros.api.users

import io.andrewohara.cheetosbros.api.auth.openxbl.OpenXblAuthResult
import io.andrewohara.cheetosbros.sources.steam.SteamSource
import java.util.*

typealias UserId = String
typealias Token = String
typealias SteamId64 = Long

interface UsersManager {

    fun createUser(): User

    fun linkSocialLogin(userId: UserId, steamId64: SteamId64): User
    fun linkSocialLogin(userId: UserId, data: OpenXblAuthResult): User

    fun linkSession(userId: UserId, sessionId: String)

    fun getUserByXuid(xuid: String): User?
    fun getUserBySteamId64(steamId64: SteamId64): User?
    fun getUserByToken(token: Token): User?
}

class InMemoryUsersManager(private val steamSource: SteamSource): UsersManager {

    private val users = mutableSetOf<User>()
    private val tokens = mutableMapOf<Token, UserId>()

    override fun createUser(): User {
        val id = UUID.randomUUID().toString()

        val user = User(id = id)
        users.add(user)
        return user
    }

    override fun linkSocialLogin(userId: UserId, data: OpenXblAuthResult): User {
        val user = users.first { it.id == userId }
        user.xbox = XboxUser(
                xuid = data.xuid,
                gamertag = data.gamertag,
                token = data.app_key
        )
        user.displayName = user.displayName ?: data.gamertag

        return user
    }

    override fun linkSocialLogin(userId: UserId, steamId64: SteamId64): User {
        val steamPlayer = steamSource.getPlayer(steamId64.toString())
        val username = steamPlayer?.displayName ?: "steam$steamId64"

        val user = users.first { it.id == userId }
        user.steam = SteamUser(
                steamId64 = steamId64,
                username = username
        )
        user.displayName = user.displayName ?: username

        return user
    }

    override fun getUserBySteamId64(steamId64: Long): User? {
        return users.firstOrNull { it.steam?.steamId64 == steamId64 }
    }

    override fun getUserByXuid(xuid: String): User? {
        return users.firstOrNull { it.xbox?.xuid == xuid }
    }

    override fun getUserByToken(token: String): User? {
        val userId = tokens[token] ?: return null
        return users.firstOrNull { it.id == userId }
    }

    override fun linkSession(userId: UserId, sessionId: String) {
        tokens[sessionId] = userId
    }
}