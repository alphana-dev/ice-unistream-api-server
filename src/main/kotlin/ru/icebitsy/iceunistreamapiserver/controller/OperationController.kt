package ru.icebitsy.iceunistreamapiserver.controller

import jakarta.validation.Valid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClientResponseException
import ru.icebitsy.iceunistreamapiserver.service.ClientService
import ru.icebitsy.iceunistreamapiserver.service.UnistreamService
import java.util.*

@RestController
@Validated
class OperationController(
    private val unistreamService: UnistreamService,
    private val clientService: ClientService
) {

    /**
     * Регистрация операции перевода карта-карта
     */
    @PostMapping("/{requestId}/{operation}")
    fun operationRegister(
        @PathVariable requestId: UUID,
        @PathVariable operation: String,
        @Valid @RequestBody requestBody: String
    )
            : ResponseEntity<Any> {
        log.info("call $requestId $operation body = $requestBody")

        try {
            val rrrr: String
            when(operation) {
                "confirm" ->  rrrr = unistreamService.confirmOperation(
                        id = requestId
                    )
                "cashtocard" -> {
                    // 1. проверяем наличие клиента в АБС (если нет, будет выброшено исключение)
                    val client = clientService.getClientById(requestBody)

                    // 2. проверяем наличие клиента в Unistream
                    var clientId = clientService.getClientUIDifClientExist(
                        unistreamService = unistreamService,
                        docSerNo = client.documentSeries+client.documentNumber)

                    // 3. если клиента нет, регистрируем
                    if (clientId.isEmpty()) {
                        clientId = clientService.registerClient(
                            unistreamService = unistreamService,
                            client = client)
                    }

                    // 4. модифицируем запрос (необходимо вставить clientUid)
                    val requestWithClientID = clientService.setNewClientContextIntoRequest(
                        cardToCardRequest = requestBody,
                        client = client,
                        newClientUid = clientId
                    )

                    // 5. регистрируем операцию
                    rrrr = unistreamService.toUnistreamOperation(
                        urlOperation = "/v2/operations/cashtocard/$requestId",
                        req = requestWithClientID,
                        httpMethod = "post"
                    )
                }
                "status" ->
                    rrrr = unistreamService.toUnistreamOperation(
                        urlOperation = "/v2/operations/$requestId",
                        req = requestBody,
                        httpMethod = "get"
                    )
                else -> throw IllegalArgumentException("Unsupported operation: $operation")

            }
            log.info("rrrrr = $rrrr")
            return ResponseEntity.ok(rrrr)
        } catch (re: WebClientResponseException) {
            val errorMessage = re.responseBodyAsByteArray.decodeToString()
            log.error(errorMessage, re)
            return ResponseEntity.ok(errorMessage)
        } catch (e: Exception) {
            log.error("cashToCardRegister exception", e)
            return ResponseEntity.ok(e)
        }
    }


    val log: Logger = LoggerFactory.getLogger(this::class.java)
}