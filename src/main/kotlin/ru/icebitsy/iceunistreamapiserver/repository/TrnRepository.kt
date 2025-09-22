package ru.icebitsy.iceunistreamapiserver.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.icebitsy.iceunistreamapiserver.entity.Trn
import java.util.*

@Repository
interface TrnRepository : JpaRepository<Trn, Long> {
    fun getByRequestUid(requestUid: UUID): Trn?
}