package ru.icebitsy.iceunistreamapiserver.crypto

import java.io.InputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

data class KeyCertPair(
    val privateKey: PrivateKey,
    val certificateChain: Array<X509Certificate>
)

object KeystoreUtils {
    // Обновленный метод, возвращающий и ключ, и сертификат
    fun loadKeyAndCertificate(
        pfxInputStream: InputStream,
        storePassword: String,
        keyAlias: String,
        keyPassword: String
    ): KeyCertPair {
        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(pfxInputStream, storePassword.toCharArray())

        val keyEntry = keyStore.getEntry(
            keyAlias,
            KeyStore.PasswordProtection(keyPassword.toCharArray())
        ) as? KeyStore.PrivateKeyEntry
            ?: throw Exception("Не удалось найти PrivateKeyEntry для алиаса: $keyAlias")

        // Извлекаем цепочку сертификатов
        val certs = keyEntry.certificateChain.map { it as X509Certificate }.toTypedArray()

        return KeyCertPair(keyEntry.privateKey, certs)
    }
}
