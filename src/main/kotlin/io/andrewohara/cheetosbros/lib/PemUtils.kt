package io.andrewohara.cheetosbros.lib

import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemReader
import java.io.Reader
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
        fun parsePEMFile(content: String): PemObject? {
            PemReader(content.reader()).use { r ->
                return r.readPemObject()
            }
        }

        fun parsePEMFile(url: URL): PemObject? {
            val content = url.openStream().reader().use {
                it.readText()
            }

            return parsePEMFile(content)
        }
    }
}