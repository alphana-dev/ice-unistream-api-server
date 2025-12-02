package ru.icebitsy.iceunistreamapiserver.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import ru.icebitsy.iceunistreamapiserver.client.UnistreamWebClient
import ru.icebitsy.iceunistreamapiserver.config.UnistreamProperties
import ru.icebitsy.iceunistreamapiserver.web.UnistreamOperation
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
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
    @Qualifier("unistreamWebClientRaw") private val webClient: WebClient,
    private val unistreamProperties: UnistreamProperties,
    private val objectMapper: ObjectMapper,
    private val signingService: SigningService
) {

    fun confirmOperation(id: UUID): String {
        // 1. Получаем операцию
        val responseStatus = toUnistreamOperation(
            req = "",
            urlOperation = "/v2/operations/$id",
            httpMethod = "get"
        )

        val unistreamOperation =
            objectMapper.readValue(responseStatus, UnistreamOperation::class.java)

        if (unistreamOperation.status != "Accepted") {
            return responseStatus
        }

        // 2. Качаем файл для подписи
        val fileBytes = try {
            log.info("Downloading sign document: {}", unistreamOperation.signDocument)
            downloadSignedDocument(unistreamOperation.signDocument)
        } catch (e: Exception) {
            log.debug(
                "Ошибка при скачивании файла по адресу {}. signDocument: {}",
                unistreamOperation.signDocument,
                e.message
            )
            throw e
        }

        // Можно убрать файлы на диск, если они не нужны реально
        File("fileForSign.bin").writeBytes(fileBytes)

        // 3. Подписываем файл
        val digitalSignature = signingService.signDetachedCms(fileBytes)
        File("fileForSign.b64").writeBytes(digitalSignature)

        // 4. Кодируем подпись в base64 и шлём confirm
        val base64EncodedString = Base64.getEncoder().encodeToString(digitalSignature)
        val confirmationBody = objectMapper.writeValueAsString(
            mapOf("confirmation" to base64EncodedString)
        )

        return toUnistreamOperation(
            req = confirmationBody,
            urlOperation = "/v2/operations/$id/confirm",
            httpMethod = "post"
        )
    }

    /**
     * Универсальный вызов Unistream API через Feign-клиент (UnistreamWebClient).
     * Возвращает body как String.
     */
    fun toUnistreamOperation(
        req: String,
        urlOperation: String,
        httpMethod: String
    ): String {
        val method = httpMethod.uppercase(Locale.getDefault())
        require(method == "GET" || method == "POST") {
            "Unsupported HTTP method: $httpMethod"
        }

        val (date, contentMd5, authorization) =
            buildAuth(method, urlOperation, if (method == "POST") req else null)

        val relativeUri = urlOperation.removePrefix("/")

        val response = when (method) {
            "POST" -> api.unistreamOperationPost(
                urlOperation = relativeUri,
                body = req,
                date = date,
                posId = unistreamProperties.posId,
                contentMd5 = contentMd5,
                authorization = authorization
            )

            "GET" -> api.unistreamOperationGet(
                urlOperation = relativeUri,
                date = date,
                posId = unistreamProperties.posId,
                contentMd5 = "",
                authorization = authorization
            )

            else -> error("Impossible branch") // уже отфильтровали выше
        }

        log.debug("response = {}", response)
        return response
    }

    /**
     * Тот же самый вызов, но через WebClient, чтобы получить ResponseEntity
     * (например, чтобы проверить конкретный HTTP статус).
     */
    fun toUnistreamOperationWithResponse(
        req: String,
        urlOperation: String,
        httpMethod: String
    ): ResponseEntity<String> {
        val method = httpMethod.uppercase(Locale.getDefault())
        require(method == "GET" || method == "POST") {
            "Unsupported HTTP method: $httpMethod"
        }

        val (date, contentMd5, authorization) =
            buildAuth(method, urlOperation, if (method == "POST") req else null)

        val builder = when (method) {
            "POST" -> webClient.post()
                .uri(urlOperation)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)

            "GET" -> webClient.get()
                .uri(urlOperation)

            else -> error("Impossible branch")
        }

        val response = builder
            .header("Date", date)
            .header("X-Unistream-Security-PosId", unistreamProperties.posId)
            .header("CONTENT-MD5", contentMd5)
            .header("Authorization", authorization)
            .header("Accept", "application/json")
            .retrieve()
            .toEntity(String::class.java)
            .block()

        log.debug("response status = {}, body = {}", response?.statusCode, response?.body)
        return response ?: ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
    }

    /**
     * Скачивание документа для подписи (GET по signDocument).
     */
    private fun downloadSignedDocument(signDocumentPath: String): ByteArray {
        val method = "GET"
        val (date, contentMd5, authorization) = buildAuth(method, signDocumentPath, null)

        return api.downloadFile(
            relativeUri = signDocumentPath.removePrefix("/"),
            date = date,
            posId = unistreamProperties.posId,
            contentMd5 = contentMd5,
            authorization = authorization
        )
    }

    /**
     * Собирает дату, Content-MD5 и Authorization (UNIHMAC ...:signature)
     * по правилам Unistream.
     */
    private fun buildAuth(
        methodRaw: String,
        urlOperation: String,
        body: String?
    ): Triple<String, String, String> {
        val method = methodRaw.uppercase(Locale.getDefault())
        val date = nowRfc1123()

        val contentMd5 = if (method == "POST" && !body.isNullOrEmpty()) {
            calculateContentMd5(body)
        } else {
            ""
        }

        val pathAndQueryLower = canonicalPath(urlOperation)
        val stringToSign = buildStringToSign(method, contentMd5, date, pathAndQueryLower)

        log.info("stringToSign: {}", stringToSign)

        val signature = sign(stringToSign)
        val authorization = "UNIHMAC ${unistreamProperties.appId}:$signature"

        return Triple(date, contentMd5, authorization)
    }

    private fun nowRfc1123(): String =
        OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME)

    private fun canonicalPath(path: String): String =
        URLDecoder.decode(path, StandardCharsets.UTF_8)
            .lowercase(Locale.getDefault())

    private fun calculateContentMd5(body: String): String {
        val md5Bytes = MessageDigest.getInstance("MD5")
            .digest(body.toByteArray(StandardCharsets.UTF_8))
        return Base64.getEncoder().encodeToString(md5Bytes)
    }

    private fun buildStringToSign(
        method: String,
        contentMd5: String,
        date: String,
        pathAndQueryLower: String
    ): String = buildString {
        append(method).append('\n')
        append(contentMd5).append('\n')
        append(date).append('\n')
        append(pathAndQueryLower).append('\n')
        append(unistreamProperties.posId) // X-UNISTREAM-HEADERS не используем — пусто
    }

    private fun sign(stringToSign: String): String {
        val secretBytes = Base64.getDecoder().decode(unistreamProperties.secret)
        val mac = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(secretBytes, "HmacSHA256"))
        }
        val raw = mac.doFinal(stringToSign.toByteArray(StandardCharsets.UTF_8))
        return Base64.getEncoder().encodeToString(raw)
    }

    companion object {
        private val log = LoggerFactory.getLogger(UnistreamService::class.java)
    }
}

/*
СТАРАЯ ВЕРСИЯ ТУТ !!!!!

@Service
class UnistreamService(
    private val api: UnistreamWebClient,
    @Qualifier("unistreamWebClientRaw") private val webClient: WebClient, // WebClient для получения ResponseEntity
    private val unistreamProperties: UnistreamProperties,
    private val objectMapper: ObjectMapper, // возьмётся из Spring (Jackson)
    private val signingService: SigningService
) {

    fun confirmOperation(id: UUID): String {

        val responseStatus = toUnistreamOperation( "", "/v2/operations/{$id}", "get")

        val unistreamOperation = objectMapper.readValue(responseStatus, UnistreamOperation::class.java)
        if (unistreamOperation.status == "Accepted") {

            val fileBytes: ByteArray = try {

                log.info("download {}", unistreamOperation.signDocument)

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

            val fileForSign = File("fileForSign.bin")
            fileForSign.writeBytes(fileBytes)


            val digitalSignature = signingService.signDetachedCms(fileBytes)
            val fileSign = File("fileForSign.b64")
            fileSign.writeBytes(digitalSignature)

            // 5. Преобразование подписи в Base64 для передачи
            val base64EncodedString = Base64.getEncoder().encodeToString(digitalSignature)

            val confirmationBody = "{\"confirmation\":\"$base64EncodedString\"}"

            val confirmationStatus = toUnistreamOperation(confirmationBody, "/v2/operations/$id/confirm", "post")
            return confirmationStatus
        }
        return responseStatus
    }

    fun toUnistreamOperation(
//        id: UUID,
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


            val pathAndQueryLower = urlOperation
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

            val pathAndQueryLower = urlOperation
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
                    urlOperation = urlOperation.substring(1),
//                    id = id,
                    body = req,
                    date = date,
                    posId = unistreamProperties.posId,
                    contentMd5 = contentMd5,
                    authorization = authorization
                )

                "get" -> api.unistreamOperationGet(
                    urlOperation = urlOperation.substring(1),
//                    id = id,
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

    /**
     * Выполняет операцию к Unistream API и возвращает ResponseEntity с HTTP статусом
     * Используется когда нужно проверить статус ответа (например, для проверки HTTP 201)
     */
    fun toUnistreamOperationWithResponse(
        req: String,
        urlOperation: String,
        httpMethod: String
    ): ResponseEntity<String> {
        // 1) Date (RFC1123, UTC)
        val date = OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME)
        val contentMd5: String
        val stringToSign: String

        if (httpMethod.lowercase() == "post") {
            val compactJson = req
            // 2) Content-MD5
            val md5Bytes = MessageDigest.getInstance("MD5").digest(compactJson.toByteArray(Charsets.UTF_8))
            contentMd5 = Base64.getEncoder().encodeToString(md5Bytes)

            val pathAndQueryLower = urlOperation
                .let { URLDecoder.decode(it, Charsets.UTF_8) }
                .lowercase()

            stringToSign = buildString {
                append("POST").append('\n')
                append(contentMd5).append('\n')
                append(date).append('\n')
                append(pathAndQueryLower).append('\n')
                append(unistreamProperties.posId)
            }
        } else {
            contentMd5 = ""
            val pathAndQueryLower = urlOperation
                .let { URLDecoder.decode(it, Charsets.UTF_8) }
                .lowercase()

            stringToSign = buildString {
                append("GET").append('\n')
                append(contentMd5).append('\n')
                append(date).append('\n')
                append(pathAndQueryLower).append('\n')
                append(unistreamProperties.posId)
            }
        }

        log.info("stringToSign: $stringToSign")
        // 6) HMAC-SHA256 + Base64
        val secret = Base64.getDecoder().decode(unistreamProperties.secret)
        val mac = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(secret, "HmacSHA256"))
        }
        val signature = Base64.getEncoder().encodeToString(mac.doFinal(stringToSign.toByteArray(Charsets.UTF_8)))
        val authorization = "UNIHMAC ${unistreamProperties.appId}:$signature"

        // Используем WebClient напрямую для получения ResponseEntity
        val response = when (httpMethod.lowercase()) {
            "post" -> webClient.post()
                .uri(urlOperation)
                .header("Date", date)
                .header("X-Unistream-Security-PosId", unistreamProperties.posId)
                .header("CONTENT-MD5", contentMd5)
                .header("Authorization", authorization)
                .header("Accept", "application/json")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .toEntity(String::class.java)
                .block()

            "get" -> webClient.get()
                .uri(urlOperation)
                .header("Date", date)
                .header("X-Unistream-Security-PosId", unistreamProperties.posId)
                .header("CONTENT-MD5", contentMd5)
                .header("Authorization", authorization)
                .header("Accept", "application/json")
                .retrieve()
                .toEntity(String::class.java)
                .block()

            else -> throw IllegalArgumentException("Unsupported HTTP method: $httpMethod")
        }

        log.debug("response status = {}, body = {}", response?.statusCode, response?.body)
        return response ?: ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
    }

    val log: Logger = LoggerFactory.getLogger(this::class.java)
}


 */