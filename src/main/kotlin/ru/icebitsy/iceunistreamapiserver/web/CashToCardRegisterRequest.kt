package ru.icebitsy.iceunistreamapiserver.web

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.util.UUID


data class CashToCardRegisterRequest (
    @JsonProperty("CardNumber")
    val cardNumber: String,
    @JsonProperty("RecipientLastName")
    val recipientLastName: String,
    @JsonProperty("RecipientFirstName")
    val recipientFirstName: String,
    @JsonProperty("AcceptedCurrency")
    val acceptedCurrency: String,
    @JsonProperty("Amount")
    val amount: BigDecimal
)
