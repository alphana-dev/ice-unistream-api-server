package ru.icebitsy.iceunistreamapiserver.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

/**
 * Сервис для обработки операций CashToCard
 */
@Service
class CashToCardService(
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

        // 1. Проверяем наличие клиента в АБС (если нет, будет выброшено исключение)
        val client = clientService.getClientById(requestBody)

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
        val requestWithClientID = clientService.setNewClientContextIntoRequest(
            cardToCardRequest = requestBody,
            client = client,
            newClientUid = clientId
        )

        // 5. Регистрируем операцию
        return unistreamService.toUnistreamOperation(
            urlOperation = "/v2/operations/cashtocard/$requestId",
            req = requestWithClientID,
            httpMethod = "post"
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(CashToCardService::class.java)
    }
}

