package ru.mtuci.drivenext
import org.junit.Assert.*
import org.junit.Test
import ru.mtuci.drivenext.domain.model.*
import java.time.Instant

class RentalRulesTest {
    private val car = CarDetails(Car(1,"BMW","530i",2500,"AT","Бензин","sedan_blue"),"Адрес","Описание",500,5000)
    private val start = Instant.parse("2030-01-01T10:00:00Z")
    private val now = start.minusSeconds(60)
    private val draft = RentalDraft(start,start.plusSeconds(86400),"Иванов Иван Иванович","1234567890")
    @Test fun oneDay() { val p = RentalRules.price(car,start,draft.end)!!; assertEquals(1L,p.days); assertEquals(3000L,p.total); assertEquals(5000L,p.deposit) }
    @Test fun partialDayRoundsUp() { assertEquals(2L,RentalRules.price(car,start,start.plusSeconds(86401))!!.days) }
    @Test fun rejectsZeroPeriod() { assertNull(RentalRules.price(car,start,start)) }
    @Test fun rejectsNegativePeriod() { assertNull(RentalRules.price(car,start,start.minusSeconds(1))) }
    @Test fun rejectsOverYear() { assertNull(RentalRules.price(car,start,start.plusSeconds(366L*86400))) }
    @Test fun rejectsPastStart() { assertFalse(RentalRules.valid(car,draft,start.plusSeconds(1))) }
    @Test fun acceptsCyrillicDriver() { assertTrue(RentalRules.valid(car,draft,now)) }
    @Test fun requiresDriver() { assertFalse(RentalRules.valid(car,draft.copy(driver=" "),now)) }
    @Test fun requiresLicense() { assertFalse(RentalRules.valid(car,draft.copy(license="123"),now)) }
    @Test fun acceptsRussianRegistrationName() { assertNull(AuthValidation.step2(RegistrationDraft(firstName="Алёна",lastName="Семёнова",middleName="Игоревна",birth="05/15/2000",gender="female"))) }
}
