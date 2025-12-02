package ru.icebitsy.iceunistreamapiserver.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import ru.icebitsy.iceunistreamapiserver.entity.Client

@Repository
interface ClientRepository : JpaRepository<Client, String> {

    /**
     * Поиск клиента по серии и номеру документа
     * @param documentSeries серия документа
     * @param documentNumber номер документа
     * @return найденный клиент или null
     */
    fun findByDocumentSeriesAndDocumentNumber(
        documentSeries: String,
        documentNumber: String
    ): Client?
}

