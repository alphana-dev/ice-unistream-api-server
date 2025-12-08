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
import ru.icebitsy.iceunistreamapiserver.service.CashToCardService
import ru.icebitsy.iceunistreamapiserver.service.UnistreamService
import java.util.*

@RestController
@Validated
class OperationController(
    private val unistreamService: UnistreamService,
    private val cashToCardService: CashToCardService
) {

    /**
     * Регистрация операции перевода карта-карта
     * @param requestId уникальный идентификатор запроса
     * @param operation тип операции (confirm, cashtocard, status)
     * @param requestBody тело запроса в формате JSON
     * @return ResponseEntity с результатом операции или ошибкой
     */
    @PostMapping("/{requestId}/{operation}")
    fun operationRegister(
        @PathVariable requestId: UUID,
        @PathVariable operation: String,
        @Valid @RequestBody requestBody: String
    ): ResponseEntity<Any> {
        log.info("call $requestId $operation body = $requestBody")

        val result: String = when (operation) {
            "confirm" -> unistreamService.confirmOperation(id = requestId)

            "cashtocard" -> cashToCardService.registerCashToCardOperation(
                requestId = requestId,
                requestBody = requestBody
            )

            "status" -> unistreamService.toUnistreamOperation(
                urlOperation = "/v2/operations/$requestId",
                req = requestBody,
                httpMethod = "get"
            )

            else -> throw IllegalArgumentException("Unsupported operation: $operation")
        }

        log.info("Operation $operation completed successfully for requestId=$requestId")
        return ResponseEntity.ok(result)
    }


    val log: Logger = LoggerFactory.getLogger(this::class.java)
}