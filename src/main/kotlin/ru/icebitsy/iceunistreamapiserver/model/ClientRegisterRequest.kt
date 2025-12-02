package ru.icebitsy.iceunistreamapiserver.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class ClientRegisterRequest(
    val id: String?,                                     // уникальный идентификатор клиента
    val countryOfResidence: String?,                     // ISO-код страны
    val firstName: String?,                              // имя
    val lastName: String?,                               // фамилия
    val middleName: String?,                             // отчество
    val gender: String?,                                 // "Male" / "Female"
    val birthPlace: String?,                             // место рождения

    // "1988-01-23T00:00:00"
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val birthDate: LocalDateTime?,                       // дата рождения

    val phoneNumber: String?,                            // номер телефона
    val taxpayerIndividualIdentificationNumber: String?, // ИНН
    val kazId: String?,                                  // ИИН (Казахстан), может быть null
    val address: Address?,                               // адрес
    val documents: List<Document>?                       // документы
)

data class Address(
    val addressString: String?,
    val apartment: String?,
    val building: String?,
    val city: String?,
    val countryCode: String?,
    val house: String?,
    val postcode: String?,
    val state: String?,
    val street: String?
)

data class Document(
    val type: String?,          // например "Passport.RUS"
    val fields: DocumentFields? // поля документа
)

data class DocumentFields(
    @JsonProperty("Series")
    val series: String?,

    @JsonProperty("Number")
    val number: String?,

    @JsonProperty("Issuer")
    val issuer: String?,

    @JsonProperty("IssuerDepartmentCode")
    val issuerDepartmentCode: String?,

    // "2008-07-07T00:00:00"
    @JsonProperty("IssueDate")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val issueDate: LocalDateTime?,

    // "2055-09-23T00:00:00"
    @JsonProperty("expiryDate")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val expiryDate: LocalDateTime?,

    @JsonProperty("state")
    val state: String?
)
