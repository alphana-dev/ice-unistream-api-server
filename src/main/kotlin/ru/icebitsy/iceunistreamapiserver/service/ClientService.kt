package ru.icebitsy.iceunistreamapiserver.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.icebitsy.iceunistreamapiserver.entity.Client
import ru.icebitsy.iceunistreamapiserver.exception.ResourceNotFoundException
import ru.icebitsy.iceunistreamapiserver.model.Address
import ru.icebitsy.iceunistreamapiserver.model.CashToCardRequest
import ru.icebitsy.iceunistreamapiserver.model.ClientRegisterRequest
import ru.icebitsy.iceunistreamapiserver.model.ClientSearchResponse
import ru.icebitsy.iceunistreamapiserver.model.Document
import ru.icebitsy.iceunistreamapiserver.model.DocumentFields
import ru.icebitsy.iceunistreamapiserver.repository.ClientRepository

@Service
class ClientService(
    private val objectMapper: ObjectMapper,
    private val clientRepository: ClientRepository
) {

    fun getClientUIDifClientExist(unistreamService: UnistreamService, cardToCardRequest: String): String {
        val docSerNo = getDocumentSerNo(cardToCardRequest)
        val urlOperation = "/v2/clients/search?documentNumber=$docSerNo"

        val response = unistreamService.toUnistreamOperation(
            req = "",
            urlOperation = urlOperation,
            httpMethod = "get"
        )
        return getClientUID(response)
    }

    /**
     * Регистрация клиента
     * @param unistreamService сервис для взаимодействия с Unistream API
     * @param cardToCardRequest запрос карта-карта
     * return ID - уникальный идентификатор клиента если регистрация удалась иначе исключение
     */
    fun registerClient(unistreamService: UnistreamService, cardToCardRequest: String): String {
        val docSerNo = getDocumentSerNo(cardToCardRequest)
        val documentSeries = docSerNo.substring(0, 4)
        val documentNumber = docSerNo.substring(4)
        val clientRegisterRequest: ClientRegisterRequest =
            findClientByDocumentAndConvertToRequest(documentSeries, documentNumber)
                ?: throw ResourceNotFoundException(
                    "Ошибка регистрации клиента. Клиент с серией номером документа $documentSeries/$documentNumber не найден в БД"
                )

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

    private fun getClientUID(response: String): String {
        return try {
            val clientSearchResponse = objectMapper.readValue(response, ClientSearchResponse::class.java)
            clientSearchResponse.id!!
        } catch (e: Exception) {
            log.error("Ошибка обработки запроса данных о клиенте. response=$response", e)
            throw RuntimeException(e)
        }

    }

    /**
     * Поиск клиента в БД по серии и номеру документа и преобразование в ClientRegisterRequest
     * @param documentSeries серия документа
     * @param documentNumber номер документа
     * @return ClientRegisterRequest или null, если клиент не найден
     */
    fun findClientByDocumentAndConvertToRequest(
        documentSeries: String,
        documentNumber: String
    ): ClientRegisterRequest? {
        val client = clientRepository.findByDocumentSeriesAndDocumentNumber(
            documentSeries = documentSeries,
            documentNumber = documentNumber
        ) ?: return null

        return convertClientEntityToRequest(client)
    }

    fun setClientUIDIntoRequest(cardToCardRequest: String, clientId: String): String {
        return try {
            val request = objectMapper.readValue(cardToCardRequest, CashToCardRequest::class.java)
            request.clientContext!!.clientId = clientId
            objectMapper.writeValueAsString(request)
        } catch (e: Exception) {
            log.error("Ошибка установки clientId для request=$cardToCardRequest", e)
            throw e
        }
    }

    /**
     * Преобразование Entity Client в ClientRegisterRequest
     */
    private fun convertClientEntityToRequest(client: Client): ClientRegisterRequest {
        // Преобразование адреса
        val address = if (client.addressString != null || client.city != null || client.street != null) {
            Address(
                addressString = client.addressString,
                apartment = client.apartment,
                building = client.building,
                city = client.city,
                countryCode = client.countryCode,
                house = client.house,
                postcode = client.postcode,
                state = client.state,
                street = client.street
            )
        } else null

        // Преобразование документа
        val documents = if (client.documentType != null) {
            listOf(
                Document(
                    type = client.documentType,
                    fields = DocumentFields(
                        series = client.documentSeries,
                        number = client.documentNumber,
                        issuer = client.documentIssuer,
                        issuerDepartmentCode = client.documentIssuerDepartmentCode,
                        issueDate = client.documentIssueDate,
                        expiryDate = client.documentExpiryDate,
                        state = client.documentState
                    )
                )
            )
        } else null

        return ClientRegisterRequest(
            id = client.clientId,
            countryOfResidence = client.countryOfResidence,
            firstName = client.firstName,
            lastName = client.lastName,
            middleName = client.middleName,
            gender = client.gender,
            birthPlace = client.birthPlace,
            birthDate = client.birthDate,
            phoneNumber = client.phoneNumber,
            taxpayerIndividualIdentificationNumber = client.taxpayerIndividualIdentificationNumber,
            kazId = client.kazId,
            address = address,
            documents = documents
        )
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