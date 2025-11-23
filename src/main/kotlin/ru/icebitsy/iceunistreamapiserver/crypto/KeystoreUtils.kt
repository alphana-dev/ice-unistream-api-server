package ru.icebitsy.iceunistreamapiserver.crypto

import java.io.InputStream
import java.security.KeyStore
import java.security.PrivateKey

object KeystoreUtils {

    // Принимает InputStream для гибкости (из File, из Classpath, откуда угодно)
    fun loadPrivateKey(
        pfxInputStream: InputStream,
        storePassword: String,
        keyAlias: String,
        keyPassword: String
    ): PrivateKey {

        val keyStore = KeyStore.getInstance("PKCS12")

        // Загрузка KeyStore из потока
        keyStore.load(pfxInputStream, storePassword.toCharArray())

        // ... остальная логика остается той же ...
        val keyEntry = keyStore.getEntry(
            keyAlias,
            KeyStore.PasswordProtection(keyPassword.toCharArray())
        ) as? KeyStore.PrivateKeyEntry
            ?: throw Exception("Не удалось найти PrivateKeyEntry для алиаса: $keyAlias")

        return keyEntry.privateKey
    }
}