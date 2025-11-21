package ru.icebitsy.iceunistreamapiserver.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.icebitsy.iceunistreamapiserver.client.UnistreamWebClient
import ru.icebitsy.iceunistreamapiserver.config.UnistreamProperties
import ru.icebitsy.iceunistreamapiserver.web.CashToCardRegisterRequest
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

    fun toUnistreamOperation(
        id: UUID,
        req: CashToCardRegisterRequest,
        urlOperation: String
    ): Any {
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
        val compactJson = objectMapper.writeValueAsString(req) // Jackson уже даёт компактный JSON

        // 2) Content-MD5
        val md5Bytes = MessageDigest.getInstance("MD5").digest(compactJson.toByteArray(Charsets.UTF_8))
        val contentMd5 = Base64.getEncoder().encodeToString(md5Bytes)

        // 3) Date (RFC1123, UTC)
        val date = OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME)

        // 4) PATH-AND-QUERY: декодируем и приводим к нижнему регистру
        val pathAndQueryLower = "/v2/operations/$urlOperation/$id"
            .let { URLDecoder.decode(it, Charsets.UTF_8) }
            .lowercase()

        // 5) StringToSign
        val stringToSign = buildString {
            append("POST").append('\n')
            append(contentMd5).append('\n')
            append(date).append('\n')
            append(pathAndQueryLower).append('\n')
            append(unistreamProperties.posId)
            // X-UNISTREAM-HEADERS — пусто, если не используешь X-Unistream-*
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

        val response = api.unistreamOperation(
            operation = urlOperation,
            id = id,
            body = req,
            date = date,
            posId = unistreamProperties.posId,
            contentMd5 = contentMd5,
            authorization = authorization
        )
        log.debug("cashToCard response = {}", response)
        // 7) вызов
        return response
    }

    val log: Logger = LoggerFactory.getLogger(this::class.java)
}
