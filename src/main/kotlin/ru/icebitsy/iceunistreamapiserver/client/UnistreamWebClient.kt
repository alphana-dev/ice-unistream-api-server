package ru.icebitsy.iceunistreamapiserver.client

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import java.util.*

//@HttpExchange(accept = ["application/json"], contentType = "application/json")
@HttpExchange(accept = ["*/*"]) // Принимаем любой тип контента
interface UnistreamWebClient {

    @PostExchange("/v2/operations/{operation}/{id}", contentType = "application/json")
    fun unistreamOperationPost(
        @PathVariable operation: String,
        @PathVariable id: UUID,
        @RequestBody body: String,
        @RequestHeader("Date") date: String,
        @RequestHeader("X-Unistream-Security-PosId") posId: String,
        @RequestHeader("CONTENT-MD5") contentMd5: String,
        @RequestHeader("Authorization") authorization: String,
        @RequestHeader("Accept") accept: String = "application/json"
    ): String

    @GetExchange("/v2/operations/{id}")
    fun unistreamOperationGet(
        @PathVariable operation: String,
        @PathVariable id: UUID,
        @RequestHeader("Date") date: String,
        @RequestHeader("X-Unistream-Security-PosId") posId: String,
        @RequestHeader("CONTENT-MD5") contentMd5: String,
        @RequestHeader("Authorization") authorization: String,
        @RequestHeader("Accept") accept: String = "application/json"
    ): String

    @GetExchange("{relativeUri}")
    fun downloadFile(
        @PathVariable relativeUri: String,
        @RequestHeader("Date") date: String,
        @RequestHeader("X-Unistream-Security-PosId") posId: String,
        @RequestHeader("CONTENT-MD5") contentMd5: String,
        @RequestHeader("Authorization") authorization: String,
        @RequestHeader("Accept") accept: String = "application/pdf"
    ): ByteArray // Возвращаем байты файла
}