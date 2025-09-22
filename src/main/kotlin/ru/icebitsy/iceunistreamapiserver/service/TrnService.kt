package ru.icebitsy.iceunistreamapiserver.service

import org.springframework.stereotype.Service
import ru.icebitsy.iceunistreamapiserver.entity.Trn
import ru.icebitsy.iceunistreamapiserver.entity.TrnStatus
import ru.icebitsy.iceunistreamapiserver.repository.TrnRepository
import ru.icebitsy.iceunistreamapiserver.web.CashToCardRegisterRequest
import java.util.*


@Service
class TrnService(private val trnRepository: TrnRepository) {

    fun registerTrn(requestId: UUID, request: CashToCardRegisterRequest): Trn {

        // проверка того, что такой запрос уже бал ранее зарегистрирован
        // если так, то вернем уже существующую операцию
        val existTrn = trnRepository.getByRequestUid(requestId)
        if (existTrn != null) {
            return existTrn
        }

        // регистрируем новую операцию
        val trn = Trn(
            requestUid = requestId,
            cardNumber = request.cardNumber,
            recipientFirstName = request.recipientFirstName,
            recipientLastName = request.recipientLastName,
            acceptedCurrency = request.acceptedCurrency,
            amount = request.amount,
            status = TrnStatus.New
        )

        val savedTrn = trnRepository.save(trn)

        return savedTrn
    }

}