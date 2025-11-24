package ru.icebitsy.iceunistreamapiserver.service

import jakarta.annotation.PostConstruct
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import ru.icebitsy.iceunistreamapiserver.crypto.CmsRsaSigner
import ru.icebitsy.iceunistreamapiserver.crypto.KeyCertPair
import ru.icebitsy.iceunistreamapiserver.crypto.KeystoreUtils
import ru.icebitsy.iceunistreamapiserver.crypto.RsaSigner
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.Security

@Service
class SigningService(
    @Value("\${keystore.location}") private val keystorePath: String,
    @Value("\${keystore.password}") private val keystorePassword: String,
    @Value("\${key.alias}") private val keyAlias: String,
    @Value("\${key.password}") private val keyPassword: String
) {

    private lateinit var keyCertPair: KeyCertPair

    @PostConstruct
    fun init() {

        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }

        try {

            val inputStream = java.io.FileInputStream(keystorePath)

            inputStream?.use { stream ->
                keyCertPair = KeystoreUtils.loadKeyAndCertificate(
                    stream,
                    keystorePassword,
                    keyAlias,
                    keyPassword
                )
            } ?: throw IllegalStateException("Файл хранилища не найден: $keystorePath")

            println("Приватный ключ успешно загружен.")
        } catch (e: Exception) {
            throw RuntimeException("Ошибка инициализации ключа подписи", e)
        }
    }

    /**
     * Формирует отсоединенную (detached) подпись CMS/PKCS#7.
     * @param dataBytes Исходные данные для подписи.
     * @return Подпись в формате CMS/PKCS#7 в виде массива байтов.
     */
    fun signDetachedCms(dataBytes: ByteArray): ByteArray {
        try {
            return CmsRsaSigner.signDataToDetachedCms(
                dataToSign = dataBytes,
                privateKey = keyCertPair.privateKey,
                certificate = keyCertPair.certificateChain.first() // Используем первый сертификат в цепочке
            )
        } catch (e: Exception) {
            throw RuntimeException("Ошибка при создании CMS/PKCS#7 подписи", e)
        }
    }

    /**
     * Формирует присоединенную (attached/enveloped) подпись CMS/PKCS#7.
     * @param data Исходные данные для подписи.
     * @return Подпись в формате CMS/PKCS#7 в виде массива байтов (содержит данные).
     */
    fun signAttachedCms(data: String): ByteArray {
        try {
            val dataBytes = data.toByteArray(StandardCharsets.UTF_8)

            return CmsRsaSigner.signDataToCms(
                dataToSign = dataBytes,
                privateKey = keyCertPair.privateKey,
                certificate = keyCertPair.certificateChain.first()
            )
        } catch (e: Exception) {
            throw RuntimeException("Ошибка при создании присоединенной CMS подписи", e)
        }
    }

}