package ru.icebitsy.iceunistreamapiserver.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("unistream")
data class UnistreamProperties (
    var baseUrl: String = "",
    var posId: String = "",
    var appId: String = "",
    var secret: String = "",
)
