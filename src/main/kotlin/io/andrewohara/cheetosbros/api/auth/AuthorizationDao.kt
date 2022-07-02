package io.andrewohara.cheetosbros.api.auth

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.onFailure
import dev.forkhandles.result4k.valueOrNull
import org.http4k.connect.amazon.core.model.Base64Blob
import org.http4k.connect.amazon.core.model.KMSKeyId
import org.http4k.connect.amazon.kms.KMS
import org.http4k.connect.amazon.kms.decrypt
import org.http4k.connect.amazon.kms.encrypt
import org.http4k.connect.amazon.kms.model.EncryptionAlgorithm
import java.io.IOException

interface AuthorizationDao {

    fun resolveUserId(token: String): String?
    fun assignToken(userId: String): String
}

class KmsAuthorizationDao(private val kms: KMS, private val keyId: KMSKeyId): AuthorizationDao {

    override fun resolveUserId(token: String): String? {
        return kms.decrypt(
            KeyId = keyId,
            EncryptionAlgorithm = EncryptionAlgorithm.RSAES_OAEP_SHA_256,
            CiphertextBlob = Base64Blob.of(token)
        )
            .map { it.Plaintext.decoded() }
            .onFailure { error -> throw IOException(error.toString()) }
//            .valueOrNull()
    }

    override fun assignToken(userId: String): String {
        return kms.encrypt(
            KeyId = keyId,
            EncryptionAlgorithm = EncryptionAlgorithm.RSAES_OAEP_SHA_256,
            Plaintext = Base64Blob.encode(userId)
        )
            .map { it.CiphertextBlob.value }
            .onFailure { throw IOException("Error assigning token: $it") }
    }

}