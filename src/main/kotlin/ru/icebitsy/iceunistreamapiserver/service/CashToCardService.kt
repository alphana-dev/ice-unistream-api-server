package ru.icebitsy.iceunistreamapiserver.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.icebitsy.iceunistreamapiserver.model.CashToCardRequest
import java.util.*

/**
 * Сервис для обработки операций CashToCard
 */
@Service
class CashToCardService(
    private val objectMapper: ObjectMapper,
    private val clientService: ClientService,
    private val unistreamService: UnistreamService
) {

    /**
     * Регистрация операции CashToCard
     * @param requestId уникальный идентификатор запроса
     * @param requestBody JSON строка с запросом CashToCardRequest
     * @return ответ от Unistream API
     */
    fun registerCashToCardOperation(requestId: UUID, requestBody: String): String {
        log.info("Регистрация операции CashToCard для requestId=$requestId")

        var requestForUnistream = requestBody

        val req = objectMapper.readValue(requestBody, CashToCardRequest::class.java)
        if (req.cusNum != null) {
            // Считываем нужные данные клиента
            val client = clientService.getClientById(req)

            // 2. Проверяем наличие клиента в Unistream
            var clientId: String? = null
            try {
                clientId = clientService.getClientUIDifClientExist(
                    unistreamService = unistreamService,
                    docSerNo = client.documentSeries + client.documentNumber
                )
            } catch (e: Exception) {
                log.info("Клиент не найден в Unistream: ${e.message}")
            }

            // 3. Если клиента нет, регистрируем
            if (clientId.isNullOrEmpty()) {
                log.info("Клиент не найден в Unistream, выполняем регистрацию")
                clientId = clientService.registerClient(
                    unistreamService = unistreamService,
                    client = client
                )
            }

            // 4. Модифицируем запрос (необходимо вставить clientUid)
            requestForUnistream = clientService.setNewClientContextIntoRequest(
                cardToCardRequest = requestBody,
                client = client,
                newClientUid = clientId
            )
        }

        // 5. Регистрируем операцию
        return unistreamService.toUnistreamOperation(
            urlOperation = "/v2/operations/cashtocard/$requestId",
            req = requestForUnistream,
            httpMethod = "post"
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(CashToCardService::class.java)
    }
}

