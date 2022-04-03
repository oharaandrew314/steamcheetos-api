package io.andrewohara.cheetosbros.lib

//import java.net.URL
//import java.security.KeyFactory
//import java.security.PrivateKey
//import java.security.PublicKey
//import java.security.spec.PKCS8EncodedKeySpec
//import java.security.spec.X509EncodedKeySpec
//
//import java.util.*
//import java.util.regex.Pattern
//
//
//class PemUtils(algorithm: String) {
//
//    private val kf = KeyFactory.getInstance(algorithm)
//
//    fun getPublicKey(key: ByteArray): PublicKey? {
//        val keySpec = X509EncodedKeySpec(key)
//        return kf.generatePublic(keySpec)
//    }
//
//    fun getPrivateKey(key: ByteArray): PrivateKey? {
//        val keySpec = PKCS8EncodedKeySpec(key)
//        return kf.generatePrivate(keySpec)
//    }
//
//    companion object {
//
//        fun parsePEMFile(content: String): ByteArray? {
//            val parse = Pattern.compile("(?m)(?s)^---*BEGIN.*---*$(.*)^---*END.*---*$.*")
//            val encoded: String = parse.matcher(content).replaceFirst("$1")
//            return Base64.getMimeDecoder().decode(encoded)
//        }
//
//        fun parsePEMFile(url: URL): ByteArray? {
//            val content = url.openStream().reader().use {
//                it.readText()
//            }
//
//            return parsePEMFile(content)
//        }
//    }
//}