package io.andrewohara.cheetosbros.api.auth

import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.kms.model.EncryptionAlgorithmSpec
import java.util.Base64

interface AuthorizationDao {

    fun resolveUserId(token: String): String?
    fun assignToken(userId: String): String
}

class KmsAuthorizationDao(private val kms: KmsClient, private val keyId: String): AuthorizationDao {

    private val encoder = Base64.getUrlEncoder()
    private val decoder = Base64.getUrlDecoder()

    override fun resolveUserId(token: String): String? {
        val cipherText = SdkBytes.fromByteArray(decoder.decode(token))

        val decrypted = kms.decrypt {
            it.keyId(keyId)
            it.encryptionAlgorithm(EncryptionAlgorithmSpec.RSAES_OAEP_SHA_256)
            it.ciphertextBlob(cipherText)
        }

        return decrypted.plaintext().asString(Charsets.UTF_8)
    }

    override fun assignToken(userId: String): String {
        val encrypted = kms.encrypt {
            it.keyId(keyId)
            it.encryptionAlgorithm(EncryptionAlgorithmSpec.RSAES_OAEP_SHA_256)
            it.plaintext(SdkBytes.fromString(userId, Charsets.UTF_8))
        }
        return encoder.encodeToString(encrypted.ciphertextBlob().asByteArray())
    }

}