package ru.icebitsy.iceunistreamapiserver.crypto

import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cms.*
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import java.security.PrivateKey
import java.security.cert.X509Certificate

object CmsRsaSigner {

    // Для SHA1withRSA в Bouncy Castle используется 'SHA1withRSA'
    private const val SIGNATURE_ALGORITHM = "SHA1withRSA"
    private const val CMS_PROVIDER = "BC"

    fun signDataToCms(
        dataToSign: ByteArray,
        privateKey: PrivateKey,
        certificate: X509Certificate
    ): ByteArray {

        // 1. Исходные данные для CMS
        val cmsData = CMSProcessableByteArray(dataToSign)

        // 2. Создание набора сертификатов
        val certStore = JcaCertStore(listOf(certificate))

        // 3. Создание SignerInfo Generator (информация о подписанте)
        val signerInfoGenerator = JcaSignerInfoGeneratorBuilder(
            JcaDigestCalculatorProviderBuilder()
                .setProvider(CMS_PROVIDER)
                .build()
        )
            // Настройка Content Signer (используем ваш приватный ключ и алгоритм)
            .build(
                JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
                    .setProvider(CMS_PROVIDER)
                    .build(privateKey),
                certificate
            )

        // 4. Создание CMS Signed Data
        val cmsGen = CMSSignedDataGenerator()
        cmsGen.addCertificates(certStore)

        cmsGen.addSignerInfoGenerator(signerInfoGenerator)

        // 5. Генерация подписанных данных (присоединенная подпись)
        // Параметр 'true' означает, что данные будут включены (присоединены) в CMS-контейнер.
        val signedData = cmsGen.generate(cmsData, true)

        return signedData.encoded
    }

    // Функция для создания ОТСОЕДИНЕННОЙ подписи (без включения данных)
    fun signDataToDetachedCms(
        dataToSign: ByteArray,
        privateKey: PrivateKey,
        certificate: X509Certificate
    ): ByteArray {
        val cmsData = CMSProcessableByteArray(dataToSign)
        val certStore = JcaCertStore(listOf(certificate))

        // 3. Создание SignerInfo Generator (информация о подписанте)
        val signerInfoGenerator = JcaSignerInfoGeneratorBuilder(
            JcaDigestCalculatorProviderBuilder()
                .setProvider(CMS_PROVIDER)
                .build()
        )
            // Настройка Content Signer (используем ваш приватный ключ и алгоритм)
            .build(
                JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
                    .setProvider(CMS_PROVIDER)
                    .build(privateKey),
                certificate
            )

        val cmsGen = CMSSignedDataGenerator()
        cmsGen.addCertificates(certStore)

        cmsGen.addSignerInfoGenerator(signerInfoGenerator)

        // Изменение: передаем 'false' для отсоединенной подписи
        val signedData = cmsGen.generate(cmsData, false)

        // Возвращаем только CMS-контейнер (он не содержит данных, только ссылку на них и подпись)
        return signedData.encoded
    }
}