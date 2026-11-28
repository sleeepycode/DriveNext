package ru.mtuci.drivenext.domain.model

import java.time.Instant
import java.time.Duration
import java.util.UUID

data class CarDetails(val car: Car, val address: String, val description: String, val insurance: Int, val deposit: Int)
data class RentalDraft(val start: Instant, val end: Instant, val driver: String = "", val license: String = "", val requestId: String = UUID.randomUUID().toString())
data class RentalPrice(val days: Long, val rent: Long, val insurance: Long, val deposit: Long) { val total get() = rent + insurance }
data class Booking(val id: Long, val car: Car, val address: String, val start: Instant, val end: Instant, val driver: String, val license: String, val status: String, val rent: Long, val insurance: Long, val deposit: Long) {
    val active get() = status == "approved" && end.isAfter(Instant.now())
    val total get() = rent + insurance
}
object RentalRules {
    fun price(details: CarDetails, start: Instant, end: Instant): RentalPrice? {
        if (!end.isAfter(start)) return null
        val duration = Duration.between(start, end)
        if (duration > Duration.ofDays(365)) return null
        val seconds = duration.seconds + if (duration.nano > 0) 1 else 0
        val days = (seconds + 86399) / 86400
        return RentalPrice(days, days * details.car.dailyPrice, days * details.insurance, details.deposit.toLong())
    }
    fun valid(details: CarDetails, draft: RentalDraft, now: Instant = Instant.now()): Boolean =
        !draft.start.isBefore(now) && price(details, draft.start, draft.end) != null && draft.driver.isNotBlank() && draft.driver.length <= 200 && draft.license.length == 10 && draft.license.none { it.isWhitespace() }
}
