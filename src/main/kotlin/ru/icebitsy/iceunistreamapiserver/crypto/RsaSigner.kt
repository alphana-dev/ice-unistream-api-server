package ru.icebitsy.iceunistreamapiserver.crypto

import java.security.PrivateKey
import java.security.Signature
import java.util.Base64

object RsaSigner {

//    private const val SIGNATURE_ALGORITHM = "SHA256withRSA"
    private const val SIGNATURE_ALGORITHM = "SHA1withRSA"

    fun signData(dataToSign: ByteArray, privateKey: PrivateKey): ByteArray {
        // 1. Создание экземпляра Signature
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM)

        // 2. Инициализация для подписи
        signature.initSign(privateKey)

        // 3. Обновление подписываемых данных
        signature.update(dataToSign)

        // 4. Генерация подписи
        val digitalSignature = signature.sign()

        return digitalSignature
    }
}