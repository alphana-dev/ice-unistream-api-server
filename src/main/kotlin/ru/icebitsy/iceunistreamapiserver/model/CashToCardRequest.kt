package ru.icebitsy.iceunistreamapiserver.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CashToCardRequest(
    var cusNum: String? = null,
    var clientContext: ClientContext? = null,
    val data: DataDto? = null
)

data class ClientContext(
    var clientId: String? = null,
    val documents: List<String>? = null
)

data class DataDto(
    @JsonProperty("CardNumber")
    val cardNumber: String? = null,

    @JsonProperty("RecipientLastName")
    val recipientLastName: String? = null,

    @JsonProperty("RecipientFirstName")
    val recipientFirstName: String? = null,

    @JsonProperty("RecipientMiddleName")
    val recipientMiddleName: String? = null,

    @JsonProperty("AcceptedCurrency")
    val acceptedCurrency: String? = null,

    @JsonProperty("Amount")
    val amount: String? = null,

    @JsonProperty("WithdrawCurrency")
    val withdrawCurrency: String? = null,

    @JsonProperty("FundsSource")
    val fundsSource: String? = null,

    @JsonProperty("OperationPurpose")
    val operationPurpose: String? = null,

    @JsonProperty("OperationAim")
    val operationAim: String? = null
)
