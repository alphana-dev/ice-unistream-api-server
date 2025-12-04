package ru.icebitsy.iceunistreamapiserver.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.icebitsy.iceunistreamapiserver.entity.Client

@Repository
interface ClientRepository : JpaRepository<Client, String> {

    /**
     * Поиск клиента по уникальному идентификатору клиента
     * @param clientId уникальный идентификатор клиента
     * @return найденный клиент или null
     */
    fun findByClientId(clientId: String): Client?
}

