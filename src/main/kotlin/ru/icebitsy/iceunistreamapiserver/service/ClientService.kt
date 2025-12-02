package ru.icebitsy.iceunistreamapiserver.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.icebitsy.iceunistreamapiserver.exception.ResourceNotFoundException
import ru.icebitsy.iceunistreamapiserver.model.CashToCardRequest
import ru.icebitsy.iceunistreamapiserver.model.ClientSearchResponse

@Service
class ClientService(
    private val objectMapper: ObjectMapper
) {

    fun isClientExist(unistreamService: UnistreamService, cardToCardRequest: String): Boolean {
        val docSerNo = getDocumentSerNo(cardToCardRequest)
        val urlOperation = "/v2/clients/search?documentNumber=$docSerNo"

        val response = unistreamService.toUnistreamOperation(
            req = "",
            urlOperation = urlOperation,
            httpMethod = "get"
        )
        return isClientExists(response)
    }

    private fun isClientExists(response: String): Boolean {
        return try {
            val clientSearchResponse = objectMapper.readValue(response, ClientSearchResponse::class.java)
            !clientSearchResponse.documents.isNullOrEmpty()
        } catch (e: Exception) {
            log.error("Ошибка обработки запроса данных о клиенте. response=$response", e)
            throw RuntimeException(e)
        }

    }

    private fun getDocumentSerNo(cardToCardRequest: String): String {
        return try {
            val prefix = "Passport.RUS."
            val request = objectMapper.readValue(cardToCardRequest, CashToCardRequest::class.java)
            val serNo = request.clientContext?.documents!!.filter { it.contains(prefix) }[0].substring(prefix.length)
            if (serNo.isEmpty()) {
                throw ResourceNotFoundException(
                    "Ошибка получения серии номера документа клиента. " +
                            "cardToCardRequest=$cardToCardRequest"
                )
            }
            serNo
        } catch (e: Exception) {
            log.error("Ошибка получения серии номера документа клиента. cardToCardRequest=$cardToCardRequest", e)
            throw e
        }
    }

    val log: Logger = LoggerFactory.getLogger(this::class.java)
}