package ru.icebitsy.iceunistreamapiserver.model

import com.fasterxml.jackson.annotation.JsonProperty

data class ClientSearchResponse(
    val documents: List<DocumentDto>? = null,
    val confidants: List<Any>? = null,
    val id: String? = null,
    val countryOfResidence: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val middleName: String? = null,
    val gender: String? = null,
    val birthPlace: String? = null,
    val birthDate: String? = null,
    val phoneNumber: String? = null,
    val taxpayerIndividualIdentificationNumber: String? = null,
    val address: AddressDto? = null,
    val loyaltyCardNumber: String? = null,
    val kazId: String? = null
)

data class DocumentDto(
    val type: String? = null,
    val uniqueNumber: String? = null,
    val legend: String? = null,
    val fields: DocumentFieldsDto? = null
)

data class DocumentFieldsDto(
    @JsonProperty("Series")
    val series: String? = null,

    @JsonProperty("Number")
    val number: String? = null,

    @JsonProperty("Issuer")
    val issuer: String? = null,

    @JsonProperty("IssuerDepartmentCode")
    val issuerDepartmentCode: String? = null,

    @JsonProperty("IssueDate")
    val issueDate: String? = null,

    @JsonProperty("state")
    val state: String? = null
)

data class AddressDto(
    val addressString: String? = null,
    val apartment: String? = null,
    val building: String? = null,
    val city: String? = null,
    val countryCode: String? = null,
    val house: String? = null,
    val postcode: String? = null,
    val state: String? = null,
    val street: String? = null
)
