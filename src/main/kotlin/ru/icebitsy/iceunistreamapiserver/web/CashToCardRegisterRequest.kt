package ru.icebitsy.iceunistreamapiserver.web

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.util.UUID


data class ClientContext(
    @JsonProperty("clientId")
    val clientId: String,
    @JsonProperty("documents")
    val documents: List<String>
)

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
    val withdrawCurrency: String,
    @JsonProperty("FundsSource")
    val fundsSource: String,
    @JsonProperty("OperationPurpose")
    val operationPurpose: String,
    @JsonProperty("OperationAim")
    val operationAim: String
)

data class CashToCardRegisterRequest(
    @JsonProperty("clientContext")
    val clientContext: ClientContext,
    @JsonProperty("data")
    val data: CashToCardRegisterRequestData
)
