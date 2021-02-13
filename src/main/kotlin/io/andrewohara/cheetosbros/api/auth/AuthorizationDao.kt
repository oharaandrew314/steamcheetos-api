package io.andrewohara.cheetosbros.api.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTDecodeException
import io.andrewohara.cheetosbros.api.users.PlayersDao
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.lib.PemUtils
import io.andrewohara.cheetosbros.sources.Platform
import org.bouncycastle.util.io.pem.PemObject
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey

interface AuthorizationDao {

    fun resolveUserId(token: String): String?
    fun assignToken(user: User): String
}

class JwtAuthorizationDao(private val issuer: String, privateKey: PemObject, publicKey: PemObject, private val playersDao: PlayersDao): AuthorizationDao {

    private val algorithm = let {
        val pemUtils = PemUtils("EC")

        Algorithm.ECDSA512(
                pemUtils.getPublicKey(publicKey) as ECPublicKey,
                pemUtils.getPrivateKey(privateKey) as ECPrivateKey
        )
    }

    override fun resolveUserId(token: String): String? {
        val verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .build()

        val decoded = try {
            verifier.verify(token)
        } catch (e: JWTDecodeException) {
            return null
        }

        return decoded.subject
    }

    override fun assignToken(user: User): String {
        val players = playersDao.listForUser(user)
        val steamPlayer = players.firstOrNull { it.platform == Platform.Steam }
        val xboxPlayer = players.firstOrNull { it.platform == Platform.Xbox }

        val builder = JWT.create().apply {
            withIssuer(issuer)
            withSubject(user.id)
            withClaim("displayName", user.displayName)
            withClaim("xboxUsername", xboxPlayer?.username)
            withClaim("xboxId", xboxPlayer?.id)
            withClaim("steamUsername", steamPlayer?.username)
            withClaim("steamId", steamPlayer?.id)
        }

        return builder.sign(algorithm)
    }
}