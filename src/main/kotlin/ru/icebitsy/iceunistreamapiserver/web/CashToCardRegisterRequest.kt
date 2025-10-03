package ru.icebitsy.iceunistreamapiserver.web

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.util.UUID


data class ClientContext(
    @JsonProperty("clientId")
    val clientId: String,
    @JsonProperty("documents")
    val documents: List<String>
)

@JsonInclude(value = JsonInclude.Include.NON_NULL)
data class CashToCardRegisterRequestData(
    @JsonProperty("CardNumber")
    val cardNumber: String,
    @JsonProperty("RecipientLastName")
    val recipientLastName: String,
    @JsonProperty("RecipientFirstName")
    val recipientFirstName: String,
    @JsonProperty("RecipientMiddleName")
    val recipientMiddleName: String,
    @JsonProperty("AcceptedCurrency")
    val acceptedCurrency: String,
    @JsonProperty("Amount")
    val amount: BigDecimal,
    @JsonProperty("WithdrawCurrency")
    val withdrawCurrency: String?=null,
    @JsonProperty("FundsSource")
    val fundsSource: String?=null,
    @JsonProperty("OperationPurpose")
    val operationPurpose: String?=null,
    @JsonProperty("OperationAim")
    val operationAim: String?=null
)

@JsonInclude(value = JsonInclude.Include.NON_NULL)
data class CashToCardRegisterRequest(
    @JsonProperty("clientContext")
    val clientContext: ClientContext?=null,
    @JsonProperty("data")
    val data: CashToCardRegisterRequestData
)
