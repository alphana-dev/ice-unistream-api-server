package ru.icebitsy.iceunistreamapiserver.filter
//
//import jakarta.validation.ConstraintViolationException
//import org.slf4j.Logger
//import org.slf4j.LoggerFactory
//import org.springframework.http.HttpStatus
//import org.springframework.http.ResponseEntity
//import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException
//import org.springframework.security.authentication.BadCredentialsException
//import org.springframework.validation.FieldError
//import org.springframework.web.bind.MissingServletRequestParameterException
//import org.springframework.web.bind.annotation.ControllerAdvice
//import org.springframework.web.bind.annotation.ExceptionHandler
//import org.springframework.web.bind.annotation.ResponseStatus
//import org.springframework.web.context.request.WebRequest
//import ru.icebitsy.iceunistreamapiserver.exception.ResourceNotFoundException
//import ru.icebitsy.iceunistreamapiserver.exception.ResourceServerException
//import ru.icebitsy.iceclient.utilssecurity.model.DataResponse
//
//@ControllerAdvice
//class GlobalExceptionHandler2 {
//    val log: Logger = LoggerFactory.getLogger(this::class.java)
//
//    @ExceptionHandler(ResourceServerException::class)
//    fun resourceServerException(
//        e: ResourceServerException
//    ): ResponseEntity<DataResponse<Any>> {
//        log.error("Exception - ${e.message}, ${e.javaClass.simpleName}, ${e.cause ?: ""}", e)
//        return ResponseEntity(
//            DataResponse(
//            code = HttpStatus.BAD_REQUEST.value(),
//            message = "Недопустимый запрос. ${e.message}",
//            details = "${e.javaClass.simpleName}  ${e.cause ?: ""}"), HttpStatus.BAD_REQUEST)
//    }
//
//    @ExceptionHandler(io.jsonwebtoken.ExpiredJwtException::class)
//    @Throws(io.jsonwebtoken.ExpiredJwtException::class)
//    fun expiredJwtException(
//        e: IllegalArgumentException
//    ): ResponseEntity<DataResponse<Any>> {
//        log.error("Exception - ${e.message}, ${e.javaClass.simpleName}, ${e.cause ?: ""}", e)
//        return ResponseEntity(
//            DataResponse(
//            code = HttpStatus.UNAUTHORIZED.value(),
//            message = "Токен доступа просрочен. ${e.message}",
//            details = "${e.javaClass.simpleName}  ${e.cause ?: ""}"), HttpStatus.UNAUTHORIZED)
//    }
//
//    @ExceptionHandler(ResourceNotFoundException::class)
//    fun resourceNotFoundException(
//        e: ResourceNotFoundException
//    ): ResponseEntity<DataResponse<Any>> {
//        log.error("Exception - ${e.message}, ${e.javaClass.simpleName}", e)
//        return ResponseEntity(
//            DataResponse(
//            code = HttpStatus.NOT_FOUND.value(),
//            message = "${e.message}",
//            details = e.javaClass.simpleName), HttpStatus.NOT_FOUND)
//    }
//
//    @ExceptionHandler(BadCredentialsException::class)
//    fun handleBadCredentialsException(e: BadCredentialsException): ResponseEntity<DataResponse<Any>> {
//        log.error("Exception - ${e.message}, ${e.javaClass.simpleName}", e)
//        return ResponseEntity(
//            DataResponse(
//            code = HttpStatus.FORBIDDEN.value(),
//            message = "${e.message} Доступ запрещен.",
//            details = e.javaClass.simpleName), HttpStatus. FORBIDDEN)
//    }
//
//    data class ValidationErrorList(
//        val errors: List<ValidationError>,
//    )
//    data class ValidationError(
//        val property: String,
//        val message: String
//    )
//    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException::class)
//    fun methodArgumentNotValidException(e: org.springframework.web.bind.MethodArgumentNotValidException, request: WebRequest): ResponseEntity<ValidationErrorList> {
//        log.error("Exception - ${e.message}, ${e.javaClass.simpleName}", e)
//        val responseStatus = e.javaClass.getAnnotation(
//            ResponseStatus::class.java
//        )
//        val status = responseStatus?.value ?: HttpStatus.BAD_REQUEST
//
//        val errors = e.bindingResult.allErrors.map { error ->
//            ValidationError(
//                property = (error as FieldError).field,
//                message = error.defaultMessage ?: "Invalid value"
//            )
//        }
//
//        return ResponseEntity(ValidationErrorList(errors = errors), status)
//    }
//
//    @ExceptionHandler(MethodArgumentNotValidException::class)
//    fun handleMethodArgumentNotValidException(
//        e: MethodArgumentNotValidException
//    ): ResponseEntity<ValidationErrorList> {
//        val errors = e.bindingResult?.fieldErrors?.map { error ->
//            ValidationError(
//                property = error.field,
//                message = error.defaultMessage ?: "Invalid value"
//            )
//        }
//
//        val response = ValidationErrorList(errors?: emptyList())
//
//        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
//    }
//
//    @ExceptionHandler(ConstraintViolationException::class)
//    fun handleConstraintViolationException(
//        e: ConstraintViolationException
//    ): ResponseEntity<ValidationErrorList> {
//        val errors = e.constraintViolations.map { violation ->
//            ValidationError(
//                property = violation.propertyPath.toString(),
//                message = violation.message
//            )
//        }
//
//        val response = ValidationErrorList(errors)
//
//        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
//    }
//
////    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
////    fun handleMethodArgumentTypeMismatchException(
////        e: MethodArgumentTypeMismatchException
////    ): ResponseEntity<ValidationErrorList> {
////        val error = ValidationError(
////            property = e.name,
////            message = "Invalid value for parameter: ${e.value}"
////        )
////        val response = ValidationErrorResponse(listOf(error))
////
////        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
////    }
//
//    @ExceptionHandler(MissingServletRequestParameterException::class)
//    fun handleMissingServletRequestParameterException(
//        e: MissingServletRequestParameterException
//    ): ResponseEntity<ValidationErrorList> {
//        val error = ValidationError(
//            property = e.parameterName,
//            message = "Missing required parameter: ${e.parameterName}"
//        )
//        val response = ValidationErrorList(listOf(error))
//
//        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
//    }
//
//    /**
//     * A general handler for all uncaught exceptions
//     */
//    @ExceptionHandler(Exception::class)
//    fun handleAllExceptions(e: Exception, request: WebRequest): ResponseEntity<DataResponse<Any>> {
//        val responseStatus = e.javaClass.getAnnotation(
//            ResponseStatus::class.java
//        )
//        val status = responseStatus?.value ?: HttpStatus.INTERNAL_SERVER_ERROR
//        val message = e.localizedMessage ?: status.reasonPhrase
//        return ResponseEntity(
//            DataResponse(
//                code = status.value(),
//                message = "Внутренняя ошибка сервиса.",
//                details = "${e.javaClass.simpleName}, $message"
//            ), status
//        )
//    }
//
//
//}
