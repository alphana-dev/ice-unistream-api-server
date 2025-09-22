package ru.icebitsy.iceunistreamapiserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import ru.icebitsy.iceunistreamapiserver.config.UnistreamProperties

@SpringBootApplication
@EnableConfigurationProperties(UnistreamProperties::class)
class IceClientCoreApplication

fun main(args: Array<String>) {
	runApplication<IceClientCoreApplication>(*args)
}
