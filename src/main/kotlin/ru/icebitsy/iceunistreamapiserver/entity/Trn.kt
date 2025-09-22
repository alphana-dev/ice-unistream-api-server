package ru.icebitsy.iceunistreamapiserver.entity

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
@Table(name = "ICE_UNISTREAM_TRN")
class Trn(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ice_unistream_trn_id_gen")
    @SequenceGenerator(name = "ice_unistream_trn_id_gen", sequenceName = "ice_unistream_trn_id_seq", allocationSize = 1)
    val id: Long? = null,

    @Column(name = "request_uid")
    val requestUid: UUID,

    @Column(name = "dcreate")
    var createDate: LocalDateTime = LocalDateTime.now(),

    @Column(name = "card_number")
    var cardNumber: String,

    @Column(name = "recipient_last_name")
    var recipientLastName: String,

    @Column(name = "recipient_first_name")
    var recipientFirstName: String,

    @Column(name = "accepted_currency")
    var acceptedCurrency: String,

    @Column(name = "amount")
    val amount: BigDecimal,

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    var status: TrnStatus,

    @Column(name = "status_date")
    var statusDate: LocalDateTime = LocalDateTime.now(),

    @Column(name = "status_message")
    var statusMessage: String? = null
)

enum class TrnStatus(val status: String) {
    New("New"),
    Created("Created"),
    Accepted("Accepted"),
    Rejected("Rejected"),
    Failed("Failed")
}