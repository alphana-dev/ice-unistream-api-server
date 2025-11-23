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
import ru.icebitsy.iceunistreamapiserver.service.UnistreamService
import java.util.*

@RestController
@Validated
class OperationController( //private val trnService: TrnService,
    private val unistreamService: UnistreamService
) {

    /**
     * Регистрация операции перевода карта-карта
     */
    @PostMapping("/{httpMethod}/{requestId}/{urlOperation}")
    fun operationRegister(
        @PathVariable httpMethod: String,
        @PathVariable requestId: UUID,
        @PathVariable urlOperation: String,
        @Valid @RequestBody requestBody: String
    )

            : ResponseEntity<Any> {
        log.info("call $httpMethod $requestId $urlOperation body = $requestBody")

        try {
            val rrrr: String
            if (urlOperation == "confirm")
                rrrr = unistreamService.confirmOperation(
                    id = requestId
                )
            else
                rrrr = unistreamService.toUnistreamOperation(
                    urlOperation = urlOperation,
                    id = requestId,
                    req = requestBody,
                    httpMethod = httpMethod
                )
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

//        val trn = trnService.registerTrn(request = cashToCardRegisterRequest)
//        if(trn.status == TrnStatus.New){
//            unistreamService.cashToCard(
//                id = cashToCardRegisterRequest.requestUid,
//                req = cashToCardRegisterRequest,
//                xUnistreamSecurityPosId = "596669"
//            )
//        }
//
//        log.debug("result call registerTransaction trn ={}", trn)
//        return ResponseEntity.ok(trn)
    }


    val log: Logger = LoggerFactory.getLogger(this::class.java)
}