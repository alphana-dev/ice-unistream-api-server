package ru.icebitsy.iceunistreamapiserver.exception

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.reactive.function.client.WebClientException
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException

/**
 * Глобальный обработчик исключений для возврата правильных HTTP кодов
 */
@ControllerAdvice
class GlobalExceptionHandler {

    /**
     * Обработка исключений от WebClient с HTTP статусом (Unistream API)
     * Пробрасываем статус код от внешнего API
     */
    @ExceptionHandler(WebClientResponseException::class)
    fun handleWebClientResponseException(e: WebClientResponseException): ResponseEntity<Any> {
        val statusCode = HttpStatus.resolve(e.statusCode.value())
            ?: HttpStatus.INTERNAL_SERVER_ERROR
        
        val errorMessage = try {
            e.responseBodyAsString
        } catch (ex: Exception) {
            "Ошибка при обращении к внешнему API ${e.message}"
        }

        log.error("WebClientResponseException: status=${statusCode.value()}, message=$errorMessage", e)
        
        // Если это 4xx ошибка от внешнего API, возвращаем 4xx
        // Если это 5xx ошибка от внешнего API, возвращаем 502 (Bad Gateway)
        val responseStatus = when {
            statusCode.is4xxClientError -> statusCode
            statusCode.is5xxServerError -> HttpStatus.BAD_GATEWAY
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }
        
        return ResponseEntity
            .status(responseStatus)
            .body(mapOf(
                "error" to errorMessage,
                "status" to responseStatus.value(),
                "message" to "Ошибка при обращении к Unistream API",
                "originalStatus" to statusCode.value()
            ))
    }

    /**
     * Обработка общих исключений WebClient (сетевые ошибки, таймауты и т.д.)
     */
    @ExceptionHandler(WebClientException::class)
    fun handleWebClientException(e: WebClientException): ResponseEntity<Any> {
        log.error("WebClientException: ${e.message}", e)
        return ResponseEntity
            .status(HttpStatus.BAD_GATEWAY)
            .body(mapOf(
                "error" to "Bad Gateway",
                "status" to HttpStatus.BAD_GATEWAY.value(),
                "message" to "Ошибка при обращении к внешнему API: ${e.message ?: "Неизвестная ошибка"}"
            ))
    }

    /**
     * Обработка ResourceNotFoundException - 404
     */
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFoundException(e: ResourceNotFoundException): ResponseEntity<Any> {
        log.error("ResourceNotFoundException: ${e.message}", e)
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(mapOf(
                "error" to "Resource Not Found",
                "status" to HttpStatus.NOT_FOUND.value(),
                "message" to (e.message ?: "Ресурс не найден")
            ))
    }

    /**
     * Обработка IllegalArgumentException - 400
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<Any> {
        log.error("IllegalArgumentException: ${e.message}", e)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(mapOf(
                "error" to "Bad Request",
                "status" to HttpStatus.BAD_REQUEST.value(),
                "message" to (e.message ?: "Некорректный запрос")
            ))
    }

    /**
     * Обработка валидации - 400
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<Any> {
        log.error("MethodArgumentNotValidException: ${e.message}", e)
        val errors = e.bindingResult.fieldErrors.map { fieldError ->
            mapOf(
                "field" to fieldError.field,
                "message" to (fieldError.defaultMessage ?: "Validation error")
            )
        }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(mapOf(
                "error" to "Validation Failed",
                "status" to HttpStatus.BAD_REQUEST.value(),
                "message" to "Ошибка валидации запроса",
                "errors" to errors
            ))
    }

    /**
     * Обработка отсутствующих параметров - 400
     */
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(e: MissingServletRequestParameterException): ResponseEntity<Any> {
        log.error("MissingServletRequestParameterException: ${e.message}", e)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(mapOf(
                "error" to "Missing Parameter",
                "status" to HttpStatus.BAD_REQUEST.value(),
                "message" to "Отсутствует обязательный параметр: ${e.parameterName}"
            ))
    }

    /**
     * Обработка всех остальных исключений - 500
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(e: Exception): ResponseEntity<Any> {
        log.error("Unexpected exception: ${e.message}", e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(mapOf(
                "error" to "Internal Server Error",
                "status" to HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "message" to "Внутренняя ошибка сервера",
                "details" to (e.message ?: e.javaClass.simpleName)
            ))
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }
}

