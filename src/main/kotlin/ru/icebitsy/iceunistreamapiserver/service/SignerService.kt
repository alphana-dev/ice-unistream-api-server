package ru.icebitsy.iceunistreamapiserver.service

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import ru.icebitsy.iceunistreamapiserver.crypto.KeystoreUtils
import ru.icebitsy.iceunistreamapiserver.crypto.RsaSigner
import java.io.File
import java.io.FileInputStream
import java.security.PrivateKey

@Service
class SigningService(
    @Value("\${keystore.location}") private val keystorePath: String,
    @Value("\${keystore.password}") private val keystorePassword: String,
    @Value("\${key.alias}") private val keyAlias: String,
    @Value("\${key.password}") private val keyPassword: String
) {

    private lateinit var signingKey: PrivateKey

    @PostConstruct
    fun init() {
        try {

            // Создаем объект File, который разрешает путь относительно рабочего каталога
            val keystoreFile = File(keystorePath)

            if (!keystoreFile.exists()) {
                throw IllegalStateException("Файл хранилища не найден по пути: ${keystoreFile.absolutePath}")
            }

            FileInputStream(keystoreFile).use { fis ->
                signingKey = KeystoreUtils.loadPrivateKey(
                    fis, // Передаем поток
                    keystorePassword,
                    keyAlias,
                    keyPassword
                )
            }
            println("Приватный ключ успешно загружен.")
        } catch (e: Exception) {
            throw RuntimeException("Ошибка инициализации ключа подписи", e)
        }
    }

    /**
     * Подписывает строковые данные и возвращает подпись в Base64.
     */
    fun sign(dataBytes: ByteArray): ByteArray {
        return RsaSigner.signData(dataBytes, signingKey)
    }
}