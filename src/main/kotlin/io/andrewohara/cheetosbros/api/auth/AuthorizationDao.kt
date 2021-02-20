package io.andrewohara.cheetosbros.api.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.InvalidClaimException
import com.auth0.jwt.exceptions.JWTDecodeException
import io.andrewohara.cheetosbros.api.users.User
import io.andrewohara.cheetosbros.lib.PemUtils
import org.bouncycastle.util.io.pem.PemObject
import org.slf4j.LoggerFactory
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey

interface AuthorizationDao {

    fun resolveUserId(token: String): String?
    fun assignToken(user: User): String
}

class JwtAuthorizationDao(private val issuer: String, privateKey: PemObject, publicKey: PemObject): AuthorizationDao {

    private val log = LoggerFactory.getLogger(javaClass)

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
            log.error("Invalid JWT", e)
            return null
        } catch (e: InvalidClaimException) {
            log.info("Invalid JWT claim", e)
            return null
        }

        return decoded.subject
    }

    override fun assignToken(user: User): String {
        val avatar = user.players.values
            .map { it.avatar }
            .firstOrNull()

        val builder = JWT.create().apply {
            withIssuer(issuer)
            withSubject(user.id)
            withClaim("displayName", user.displayName())
            withClaim("avatar", avatar)
        }

        return builder.sign(algorithm)
    }
}