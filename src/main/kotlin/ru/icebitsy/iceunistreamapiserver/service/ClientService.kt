package ru.icebitsy.iceunistreamapiserver.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.icebitsy.iceunistreamapiserver.entity.Client
import ru.icebitsy.iceunistreamapiserver.exception.ResourceNotFoundException
import ru.icebitsy.iceunistreamapiserver.mapper.ClientMapper
import ru.icebitsy.iceunistreamapiserver.model.*
import ru.icebitsy.iceunistreamapiserver.repository.ClientRepository

@Service
class ClientService(
    private val objectMapper: ObjectMapper,
    private val clientRepository: ClientRepository,
    private val clientMapper: ClientMapper
) {

    /**
     * Получение уникального идентификатора клиента по номеру документа
     * @param unistreamService сервис для взаимодействия с Unistream API
     * @param docSerNo номер документа (серия + номер)
     * @return ID - уникальный идентификатор клиента если клиент существует иначе пустая строка
     */
    fun getClientUIDifClientExist(unistreamService: UnistreamService, docSerNo: String): String {
        val urlOperation = "/v2/clients/search?documentNumber=$docSerNo"

        val response = unistreamService.toUnistreamOperation(
            req = "",
            urlOperation = urlOperation,
            httpMethod = "get"
        )
        return getClientUID(response)
    }

    /**
     * Регистрация клиента через ClientContext
     * @param unistreamService сервис для взаимодействия с Unistream API
     * @param client Entity клиента из БД
     * @return ID - уникальный идентификатор клиента если регистрация удалась иначе исключение
     */
    fun registerClient(unistreamService: UnistreamService, client: Client): String {
        // Преобразуем Entity в ClientRegisterRequest
        val clientRegisterRequest = clientMapper.toClientRegisterRequest(client)

        // Регистрируем клиента через Unistream API
        val requestBody = objectMapper.writeValueAsString(clientRegisterRequest)
        val response = unistreamService.toUnistreamOperationWithResponse(
            urlOperation = "/v2/clients",
            req = requestBody,
            httpMethod = "post"
        )

        // Проверяем, что ответ от сервера = HTTP 201 (Created)
        val isSuccess = response.statusCode.value() == 201

        if (isSuccess) {
            log.info("Клиент успешно зарегистрирован. Response body: ${response.body}")
        } else {
            log.warn("Ошибка регистрации клиента. HTTP статус: ${response.statusCode.value()}, Response body: ${response.body}")
        }

        return clientRegisterRequest.id!!
    }

    /**
     * Формируем новый clientContext с новым clientId и вставляем его в запрос
     * @param cardToCardRequest JSON строка с запросом CashToCardRequest
     * @param client Entity клиента из БД
     * @param newClientUid новый уникальный идентификатор клиента для вставки в запрос
     * @return модифицированный JSON запрос с новым clientContext
     */
    fun setNewClientContextIntoRequest(cardToCardRequest: String, client: Client, newClientUid: String): String {
        return try {
            val cashToCardRequest = objectMapper.readValue(cardToCardRequest, CashToCardRequest::class.java)
            cashToCardRequest.cusNum = null // очищаем cusNum, чтобы не было конфликта в запросе к unistream
            val clientContext = clientMapper.toClientContext(client).apply {
                clientId = newClientUid
            }
            cashToCardRequest.clientContext = clientContext
            objectMapper.writeValueAsString(cashToCardRequest)
        } catch (e: Exception) {
            log.error("Ошибка установки clientId для request=$cardToCardRequest", e)
            throw e
        }
    }

    /**
     * Получение клиента из БД по cusNum из запроса
     * @param cardToCardRequestJson JSON строка с запросом CashToCardRequest
     * @return найденный клиент или исключение
     */
    fun getClientById(cardToCardRequestJson: String): Client {
        return try {
            val req = objectMapper.readValue(cardToCardRequestJson, CashToCardRequest::class.java)
            val cusNum = req.cusNum
                ?: throw IllegalArgumentException("cusNum is null in request: $cardToCardRequestJson")
            clientRepository.findByClientId(cusNum)
                ?: throw ResourceNotFoundException(
                    "Клиент с clientId=$cusNum не найден в БД"
                )
        } catch (e: Exception) {
            log.error("Ошибка получения Client из запроса. cardToCardRequest=$cardToCardRequestJson", e)
            throw e
        }
    }

    private fun getClientUID(response: String): String {
        return try {
            val clientSearchResponse = objectMapper.readValue(response, ClientSearchResponse::class.java)
            clientSearchResponse.id!!
        } catch (e: Exception) {
            log.error("Ошибка обработки запроса данных о клиенте. response=$response", e)
            throw RuntimeException(e)
        }

    }

    val log: Logger = LoggerFactory.getLogger(this::class.java)
}