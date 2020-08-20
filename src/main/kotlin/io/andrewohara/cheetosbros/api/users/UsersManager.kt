package io.andrewohara.cheetosbros.api.users

import java.util.*

class UsersManager(private val usersDao: UsersDao) {

    operator fun get(cheetosUserId: String): User? {
        return usersDao[cheetosUserId]
    }

    operator fun get(socialLink: SocialLink): User? {
        return usersDao[socialLink.platform, socialLink.id]
    }

    fun createUser(socialLink: SocialLink): User {
        val id = UUID.randomUUID().toString()

        val user = User(id = id, displayName = socialLink.username).withSocialLink(socialLink)
        usersDao.save(user)
        return user
    }

    fun linkSocialLogin(user: User, socialLink: SocialLink): User {
        val updated = user.withSocialLink(socialLink)
        usersDao.save(updated)
        return updated
    }
}