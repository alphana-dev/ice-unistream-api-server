package ru.icebitsy.iceunistreamapiserver.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.icebitsy.iceunistreamapiserver.client.UnistreamWebClient
import ru.icebitsy.iceunistreamapiserver.config.UnistreamProperties
import ru.icebitsy.iceunistreamapiserver.web.UnistreamOperation
import java.net.URLDecoder
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class UnistreamService(
    private val api: UnistreamWebClient,
    private val unistreamProperties: UnistreamProperties,
    private val objectMapper: ObjectMapper, // возьмётся из Spring (Jackson)
) {

    fun confirmOperation(id: UUID): String {

        val responseStatus = toUnistreamOperation(id, "", "", "get")

        val unistreamOperation = objectMapper.readValue(responseStatus, UnistreamOperation::class.java)
        if (unistreamOperation.status == "Accepted") {

            val fileBytes: ByteArray = try {

                log.info("download {}", unistreamOperation.signDocument);

                val date = OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME)
                val pathAndQueryLower = unistreamOperation.signDocument
                    .let { URLDecoder.decode(it, Charsets.UTF_8) }
                    .lowercase()

                // 5) StringToSign
                val stringToSign = buildString {
                    append("GET").append('\n')
                    append("").append('\n')
                    append(date).append('\n')
                    append(pathAndQueryLower).append('\n')
                    append(unistreamProperties.posId)
                }

                log.info("stringToSign: $stringToSign")
                // 6) HMAC-SHA256 + Base64
                val secret = Base64.getDecoder().decode(unistreamProperties.secret)
                val mac = Mac.getInstance("HmacSHA256").apply {
                    init(SecretKeySpec(secret, "HmacSHA256"))
                }

                //        val mac = Mac.getInstance("HmacSHA256").apply {
//            init(SecretKeySpec(unistreamProperties.secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
//        }
                val signature = Base64.getEncoder().encodeToString(mac.doFinal(stringToSign.toByteArray(Charsets.UTF_8)))

//        "Authorization: UNIHMAC " + APPLICATION_ID + ":" +
//                base64(hmac-sha256(APPLICATION_SECRET,
//                    to-upper(VERB) + "\n"
//                            + CONTENT-MD5 + "\n"
//                            + DATE + "\n"
//                            + to-lower(url-decode(PATH-AND-QUERY))
//                            + X-UNISTREAM-HEADERS ))

                val authorization = "UNIHMAC ${unistreamProperties.appId}:$signature"

                api.downloadFile(
                    relativeUri = unistreamOperation.signDocument.substring(1),
                    date = date,
                    posId = unistreamProperties.posId,
                    contentMd5 = "",
                    authorization = authorization
                )

            } catch (e: Exception) {
                // Обработка ошибок сети/HTTP (4xx, 5xx) при скачивании
                log.debug("Ошибка при скачивании файла по адресу {}.signDocument: {}", unistreamOperation, e.message)
                throw e
            }

            // 2. Кодирование скачанных данных в Base64
            val base64EncodedString: String = Base64.getEncoder().encodeToString(fileBytes)

            val confirmationBody = "{\"confirmation\":\"$base64EncodedString\"}"

            val confirmationStatus = toUnistreamOperation(id, confirmationBody, "confirm", "post")
            return confirmationStatus
        }
        return responseStatus;
    }

    fun toUnistreamOperation(
        id: UUID,
        req: String,
        urlOperation: String,
        httpMethod: String
    ): String {
//        VERB
//        CONTENT-MD5
//        DATE
//        PATH-AND
//        QUERY
//        X-UNISTREAM
//        HEADERS
//        Название http метода заглавными буквами
//        Хэш код md5 содержимого запроса, для GET запросов - пустая строка
//        Заголовок дата запроса в универсальном формате RFC1123
//        URL-декодированный путь к ресурсу строчными буквами
//                Описание
//        Канкатенация значений заголовков, соответствующих маске X-Unistream-*. Заголовки сортируются по возрастанию по названию, приведенному к строчным буквам.
//        Значение каждого заголовка начинается с символа "\n"


        /*//////////////
                "Authorization: UNIHMAC " + APPLICATION_ID + ":" +

                        base64(hmac-sha256(APPLICATION_SECRET,
                            to-upper(VERB) + "\n"
                                    + CONTENT-MD5 + "\n"
                                    + DATE + "\n"
                                    + to-lower(url-decode(PATH-AND-QUERY))
                                    + X-UNISTREAM-HEADERS ))
         */////////

        // 1) каноничный JSON без пробелов
//        val compactJson = objectMapper.writeValueAsString(req) // Jackson уже даёт компактный JSON

        // 3) Date (RFC1123, UTC)
        val date = OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME)
        val contentMd5: String
        val stringToSign: String

        if (httpMethod.lowercase() == "post") {

            val compactJson = req

            // 2) Content-MD5
            val md5Bytes = MessageDigest.getInstance("MD5").digest(compactJson.toByteArray(Charsets.UTF_8))
            contentMd5 = Base64.getEncoder().encodeToString(md5Bytes)


            val pathAndQueryLower = "/v2/operations/$urlOperation/$id"
                .let { URLDecoder.decode(it, Charsets.UTF_8) }
                .lowercase()

            stringToSign = buildString {
                append("POST").append('\n')
                append(contentMd5).append('\n')
                append(date).append('\n')
                append(pathAndQueryLower).append('\n')
                append(unistreamProperties.posId)
                // X-UNISTREAM-HEADERS — пусто, если не используешь X-Unistream-*
            }
        } else {

            contentMd5 = ""

            val pathAndQueryLower = "/v2/operations/$id"
                .let { URLDecoder.decode(it, Charsets.UTF_8) }
                .lowercase()

            // 5) StringToSign
            stringToSign = buildString {
                append("GET").append('\n')
                append(contentMd5).append('\n')
                append(date).append('\n')
                append(pathAndQueryLower).append('\n')
                append(unistreamProperties.posId)
                // X-UNISTREAM-HEADERS — пусто, если не используешь X-Unistream-*
            }
        }

        log.info("stringToSign: $stringToSign")
        // 6) HMAC-SHA256 + Base64
        val secret = Base64.getDecoder().decode(unistreamProperties.secret)
        val mac = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(secret, "HmacSHA256"))
        }

        //        val mac = Mac.getInstance("HmacSHA256").apply {
//            init(SecretKeySpec(unistreamProperties.secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
//        }
        val signature = Base64.getEncoder().encodeToString(mac.doFinal(stringToSign.toByteArray(Charsets.UTF_8)))

//        "Authorization: UNIHMAC " + APPLICATION_ID + ":" +
//                base64(hmac-sha256(APPLICATION_SECRET,
//                    to-upper(VERB) + "\n"
//                            + CONTENT-MD5 + "\n"
//                            + DATE + "\n"
//                            + to-lower(url-decode(PATH-AND-QUERY))
//                            + X-UNISTREAM-HEADERS ))

        val authorization = "UNIHMAC ${unistreamProperties.appId}:$signature"

        val response =
            when (httpMethod) {
                "post" -> api.unistreamOperationPost(
                    operation = urlOperation,
                    id = id,
                    body = req,
                    date = date,
                    posId = unistreamProperties.posId,
                    contentMd5 = contentMd5,
                    authorization = authorization
                )

                "get" -> api.unistreamOperationGet(
                    operation = urlOperation,
                    id = id,
                    date = date,
                    posId = unistreamProperties.posId,
                    "",
                    authorization = authorization
                )

                else -> throw IllegalArgumentException("Unsupported HTTP method: $httpMethod") // Обработка других методов
            }

        log.debug("response = {}", response)
        // 7) вызов
        return response
    }

    val log: Logger = LoggerFactory.getLogger(this::class.java)
}
