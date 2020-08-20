package io.andrewohara.cheetosbros.api.users

import io.andrewohara.cheetosbros.sources.Game
import java.util.*

class UsersManager(private val usersDao: UsersDao) {

    operator fun get(cheetosUserId: String): User? {
        return usersDao[cheetosUserId]
    }

    operator fun get(socialLink: SocialLink): User? {
        return usersDao[socialLink.platform, socialLink.id]
    }

    fun createUser(): User {
        val id = UUID.randomUUID().toString()

        val user = User(id = id)
        usersDao.save(user)
        return user
    }

    fun linkSocialLogin(user: User, socialLink: SocialLink): User {
        val updated = when(socialLink.platform) {
            Game.Platform.Xbox -> user.copy(xbox = socialLink, displayName = user.displayName ?: socialLink.username)
            Game.Platform.Steam -> user.copy(steam = socialLink, displayName = user.displayName ?: socialLink.username)
        }

        usersDao.save(updated)

        return user
    }
}