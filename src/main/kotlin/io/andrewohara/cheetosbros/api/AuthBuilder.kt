package io.andrewohara.cheetosbros.api

import io.andrewohara.cheetosbros.api.auth.AuthManager
import io.andrewohara.cheetosbros.api.auth.JwtAuthorizationDao
import io.andrewohara.cheetosbros.api.users.SocialLinkDao
import io.andrewohara.cheetosbros.api.users.UsersDao
import io.andrewohara.cheetosbros.lib.PemUtils

object AuthBuilder {

    fun buildJwt(publicKeyIssuer: String, privateKey: String, publicKey: String, socialLinkDao: SocialLinkDao, usersDao: UsersDao): AuthManager {
        val authorizationDao = JwtAuthorizationDao(
            issuer = publicKeyIssuer,
            privateKey = PemUtils.parsePEMFile(privateKey)!!,
            publicKey = PemUtils.parsePEMFile(publicKey)!!
        )

        return AuthManager(authorizationDao, usersDao, socialLinkDao)
    }
}