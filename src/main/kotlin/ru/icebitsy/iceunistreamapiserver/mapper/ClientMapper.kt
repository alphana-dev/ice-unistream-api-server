package ru.icebitsy.iceunistreamapiserver.mapper

import org.springframework.stereotype.Component
import ru.icebitsy.iceunistreamapiserver.entity.Client
import ru.icebitsy.iceunistreamapiserver.exception.ResourceNotFoundException
import ru.icebitsy.iceunistreamapiserver.model.Address
import ru.icebitsy.iceunistreamapiserver.model.ClientContext
import ru.icebitsy.iceunistreamapiserver.model.ClientRegisterRequest
import ru.icebitsy.iceunistreamapiserver.model.Document
import ru.icebitsy.iceunistreamapiserver.model.DocumentFields

/**
 * Маппер для преобразования Entity Client в различные DTO
 */
@Component
class ClientMapper {

    /**
     * Преобразование Client Entity в ClientContext
     * @param client Entity клиента из БД
     * @return ClientContext с clientId и списком документов
     */
    fun toClientContext(client: Client): ClientContext {
        // Формируем список документов в формате "Passport.{documentType}.{series}{number}"
        val documents = if (client.documentType != null &&
            client.documentSeries.isNotEmpty() &&
            client.documentNumber.isNotEmpty()
        ) {
            val documentString = "Passport.${client.documentType}.${client.documentSeries}${client.documentNumber}"
            listOf(documentString)
        } else {
            throw ResourceNotFoundException("Недостаточно данных для формирования документов клиента. $client")
            //emptyList()
        }

        return ClientContext(
            clientId = client.clientId,
            documents = documents.takeIf { it.isNotEmpty() }
        )
    }

    /**
     * Преобразование Client Entity в ClientRegisterRequest
     * @param client Entity клиента из БД
     * @return ClientRegisterRequest для регистрации клиента в Unistream API
     */
    fun toClientRegisterRequest(client: Client): ClientRegisterRequest {
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
}

