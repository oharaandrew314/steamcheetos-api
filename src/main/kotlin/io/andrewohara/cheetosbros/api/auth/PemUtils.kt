package io.andrewohara.cheetosbros.api.auth

import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemReader
import java.net.URL
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec


class PemUtils(algorithm: String) {

    private val kf = KeyFactory.getInstance(algorithm)

    fun getPublicKey(key: PemObject): PublicKey? {
        val keySpec = X509EncodedKeySpec(key.content)
        return kf.generatePublic(keySpec)
    }

    fun getPrivateKey(key: PemObject): PrivateKey? {
        val keySpec = PKCS8EncodedKeySpec(key.content)
        return kf.generatePrivate(keySpec)
    }

    companion object {
        fun parsePEMFile(url: URL): PemObject? {
            PemReader(url.openStream().reader()).use { reader ->
                return reader.readPemObject()
            }
        }
    }
}