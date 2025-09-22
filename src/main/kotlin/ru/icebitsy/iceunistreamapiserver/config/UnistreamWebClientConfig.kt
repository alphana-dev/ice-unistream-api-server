package ru.icebitsy.iceunistreamapiserver.config

import io.netty.channel.ChannelOption
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ssl.SslBundles
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.ProxyProvider
import reactor.util.retry.Retry
import ru.icebitsy.iceunistreamapiserver.client.UnistreamWebClient
import java.time.Duration

/*
@Configuration
class PayberryClientConfig(
    private val sslBundles: SslBundles,
    @Value("\${unistream.base-url}") private val baseUrl: String,
    @Value("\${proxy.host}") private val proxyHost: String?,
    @Value("\${proxy.port:8080}") private val proxyPort: Int,
    @Value("\${proxy.enabled:false}") private val isProxyEnabled: Boolean
) {

    @Bean
    fun unistreamWebClient(): UnistreamWebClient {
        // Получаем SSL bundle по имени
        val bundle = sslBundles.getBundle("unistream-mtls")

        // Создаем HttpClient с SSL контекстом из bundle
        val httpClientBuilder = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .responseTimeout(Duration.ofSeconds(60))
            .secure { spec ->
                spec.sslContext(bundle.createSslContext())
            }

        // Добавляем прокси если нужно
        val httpClient = if (isProxyEnabled && !proxyHost.isNullOrBlank()) {
            log.debug("Активированы настройки прокси сервера: $proxyHost:$proxyPort")
            httpClientBuilder.proxy { proxy ->
                proxy.type(ProxyProvider.Proxy.HTTP)
                    .host(proxyHost)
                    .port(proxyPort)
            }
        } else {
            httpClientBuilder
        }

        // Создаем WebClient
        val client = WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024) // 20 МБ
            }
            .filter(loggingFilter())
            .build()

        // Создаем HTTP service proxy
        val factory = HttpServiceProxyFactory
            .builderFor(WebClientAdapter.create(client))
            .build()

        return factory.createClient(UnistreamWebClient::class.java)
    }

    private fun loggingFilter(): ExchangeFilterFunction {
        return ExchangeFilterFunction.ofRequestProcessor { request ->
            log.debug("Отправка запроса: ${request.method()} ${request.url()}")
            Mono.just(request)
        }.andThen(ExchangeFilterFunction.ofResponseProcessor { response ->
            log.debug("Получен ответ: ${response.statusCode()}")
            Mono.just(response)
        })
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(PayberryClientConfig::class.java)
    }
}
*/

@Configuration
class PayberryClientConfig(
    private val sslBundles: SslBundles,
    @Value("\${unistream.base-url}") private val baseUrl: String,
    @Value("\${proxy.host}") private val proxyHost: String?,
    @Value("\${proxy.port:8080}") private val proxyPort: Int,
    @Value("\${proxy.enabled:false}") private val isProxyEnabled: Boolean
) {
    @Bean
    fun unistreamWebClient(): UnistreamWebClient {

        val bundle = sslBundles.getBundle("unistream-mtls")

        // ✨ СТРОИМ Netty SslContext из менеджеров bundle
        val managers = bundle.managers
        val nettySslContext: SslContext = SslContextBuilder.forClient()
            .keyManager(managers.keyManagerFactory)          // клиентский cert+key из .pfx
            .trustManager(managers.trustManagerFactory)      // truststore (или системный, если так сконфигурено)
            .build()

        val httpClient = if (isProxyEnabled) {
            require(!proxyHost.isNullOrBlank()) { "proxyHost не может быть пустым при включенном прокси" }
            log.debug("Активированы настройки прокси сервера: $proxyHost:$proxyPort")

            HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .responseTimeout(Duration.ofSeconds(60))
                .secure { ssl -> ssl.sslContext(nettySslContext) }
                .proxy { proxy ->
                    proxy.type(ProxyProvider.Proxy.HTTP)
                        .host(proxyHost)
                        .port(proxyPort)
                }
        } else {
            HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .responseTimeout(Duration.ofSeconds(60))
                .secure { ssl -> ssl.sslContext(nettySslContext) }
        }


        val client = WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024) // 20 МБ
            }
            .filter { request, next ->
                next.exchange(request)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))) // Повторные попытки
                    .doOnError { error ->
                        log.error("Ошибка при выполнении запроса: ${error?.message}", error)
                    }
            }
            .build()

        val factory = HttpServiceProxyFactory
            .builderFor(WebClientAdapter.create(client))
            .build()
        return factory.createClient(UnistreamWebClient::class.java)
    }


    val log: Logger = LoggerFactory.getLogger(this::class.java)
}

