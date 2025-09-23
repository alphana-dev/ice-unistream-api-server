package ru.icebitsy.iceunistreamapiserver.client

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import ru.icebitsy.iceunistreamapiserver.web.CashToCardRegisterRequest
import java.util.*

@HttpExchange(accept = ["application/json"], contentType = "application/json")
interface UnistreamWebClient {
    @PostExchange("/v2/operations/cashtocard/{id}")
    fun cashToCard(
        @PathVariable id: UUID,
        @RequestBody body: CashToCardRegisterRequest,
        @RequestHeader("Date") date: String,
        @RequestHeader("X-Unistream-Security-PosId") posId: String,
        @RequestHeader("Authorization") authorization: String
    ): Any
}