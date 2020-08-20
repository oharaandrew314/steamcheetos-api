package io.andrewohara.cheetosbros.api.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTDecodeException
import io.andrewohara.cheetosbros.api.users.User
import org.bouncycastle.util.io.pem.PemObject
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey

interface AuthorizationDao {

    fun resolveUserId(token: String): String?
    fun assignToken(user: User): String
}

class JwtAuthorizationDao(private val issuer: String, privateKey: PemObject, publicKey: PemObject): AuthorizationDao {

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

        for (claim in decoded.claims) {
            println("${claim.key}:${claim.value.asString()}")
        }

        return decoded.subject
    }

    override fun assignToken(user: User): String {
        val builder = JWT.create().apply {
            withIssuer(issuer)
            withSubject(user.id)
            withClaim("displayName", user.displayName)
            if (user.xbox != null) withClaim("xboxId", user.xbox.id)
            if (user.steam != null) withClaim("steamId", user.steam.id)
        }

        return builder.sign(algorithm)
    }
}