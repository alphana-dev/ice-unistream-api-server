package ru.icebitsy.iceunistreamapiserver.config

import io.netty.channel.ChannelOption
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ssl.SslBundles
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.ProxyProvider
import reactor.util.retry.Retry
import ru.icebitsy.iceunistreamapiserver.client.UnistreamWebClient
import java.time.Duration

@Configuration
class UnistreamClientConfig(
    private val sslBundles: SslBundles,
    @Value("\${unistream.base-url}") private val baseUrl: String,
    @Value("\${proxy.host:}") private val proxyHost: String?,
    @Value("\${proxy.port:8080}") private val proxyPort: Int,
    @Value("\${proxy.enabled:false}") private val isProxyEnabled: Boolean
) {
    @Bean
    fun unistreamWebClient(): UnistreamWebClient {
        // 1) Получаем bundle по имени из yml
        val bundle = sslBundles.getBundle("unistream-mtls")
        val managers = bundle.managers

        // 2) Сборка Netty SslContext с учётом наличия key/trust менеджеров
        val builder = SslContextBuilder.forClient()
        managers.keyManagerFactory?.let { builder.keyManager(it) }          // client cert + key
        managers.trustManagerFactory?.let { builder.trustManager(it) }      // truststore (если задан)
        val nettySslContext: SslContext = builder.build()

        // 3) HttpClient (+ прокси по желанию)
        val baseClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
            .responseTimeout(Duration.ofSeconds(60))
            .secure { ssl -> ssl.sslContext(nettySslContext) }

        val httpClient = if (isProxyEnabled) {
            require(!proxyHost.isNullOrBlank()) { "proxyHost не может быть пустым при включенном прокси" }
            baseClient.proxy { p ->
                p.type(ProxyProvider.Proxy.HTTP)
                    .host(proxyHost)
                    .port(proxyPort)
            }
        } else baseClient

        val client = WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .codecs { it.defaultCodecs().maxInMemorySize(20 * 1024 * 1024) }
            .filter { req, next ->
                next.exchange(req)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .doOnError { e -> log.error("Ошибка при выполнении запроса: ${e.message}", e) }
            }
            .build()

        val factory = HttpServiceProxyFactory.builderFor(WebClientAdapter.create(client)).build()
        return factory.createClient(UnistreamWebClient::class.java)
    }

    val log: Logger = LoggerFactory.getLogger(this::class.java)
}

