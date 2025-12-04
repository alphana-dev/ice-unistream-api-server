package ru.icebitsy.iceunistreamapiserver.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "ICE_UNISTREAM_CLIENTS", schema = "XXI")
class Client(
    @Id
    @Column(name = "CLIENT_ID")
    val clientId: String, // уникальный идентификатор клиента (для Unistream)

    @Column(name = "COUNTRY_OF_RESIDENCE")
    val countryOfResidence: String?, // ISO-код страны

    @Column(name = "FIRST_NAME")
    val firstName: String?,

    @Column(name = "LAST_NAME")
    val lastName: String?,

    @Column(name = "MIDDLE_NAME")
    val middleName: String?,

    @Column(name = "GENDER")
    val gender: String?, // "Male" / "Female"

    @Column(name = "BIRTH_PLACE")
    val birthPlace: String?,

    @Column(name = "BIRTH_DATE")
    val birthDate: LocalDateTime?,

    @Column(name = "PHONE_NUMBER")
    val phoneNumber: String?,

    @Column(name = "T_I_NUMBER")
    val taxpayerIndividualIdentificationNumber: String?, // ИНН

    @Column(name = "KAZ_ID")
    val kazId: String?, // ИИН (Казахстан)

    // Адрес
    @Column(name = "ADDRESS_STRING")
    val addressString: String?,

    @Column(name = "APARTMENT")
    val apartment: String?,

    @Column(name = "BUILDINGS")
    val building: String?,

    @Column(name = "CITY")
    val city: String?,

    @Column(name = "COUNTRY_CODE")
    val countryCode: String?,

    @Column(name = "HOUSE")
    val house: String?,

    @Column(name = "POSTCODE")
    val postcode: String?,

    @Column(name = "STATE")
    val state: String?,

    @Column(name = "STREET")
    val street: String?,

    // Документ (серия и номер для поиска)
    @Column(name = "DOCUMENT_TYPE")
    val documentType: String?, // например "Passport.RUS"

    @Column(name = "DOCUMENT_SERIES")
    val documentSeries: String, // серия документа

    @Column(name = "DOCUMENT_NUMBER")
    val documentNumber: String, // номер документа

    @Column(name = "DOCUMENT_ISSUER")
    val documentIssuer: String?,

    @Column(name = "D_ISSUER_D_CODE")
    val documentIssuerDepartmentCode: String?,

    @Column(name = "DOCUMENT_ISSUE_DATE")
    val documentIssueDate: LocalDateTime?,

    @Column(name = "DOCUMENT_EXPIRY_DATE")
    val documentExpiryDate: LocalDateTime?,

    @Column(name = "DOCUMENT_STATE")
    val documentState: String?,

    @Column(name = "CREATE_DATE")
    val createDate: LocalDateTime = LocalDateTime.now()
)

