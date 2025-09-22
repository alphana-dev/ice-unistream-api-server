package ru.icebitsy.iceunistreamapiserver.web

import java.time.LocalDateTime

data class RegisterTrnResponce (
    val requestId: String,
    val status: String,
    val statusData: LocalDateTime,

    val msgId: String? = null, //"20200804600000200000000000000196",
    val bizMsgId: String? = null //"A0217115915664010000042B92BD9A0F"
)

