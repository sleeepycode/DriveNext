package ru.mtuci.drivenext

import org.junit.Assert.*
import org.junit.Test
import ru.mtuci.drivenext.domain.model.AuthValidation
import ru.mtuci.drivenext.domain.model.RegistrationDraft

class AuthValidationTest {
    private val valid = RegistrationDraft(email="student@example.com", password="demoPass123", confirm="demoPass123",
        consent=true, firstName="Иван", lastName="Тестов", birth="02/29/2000", gender="male",
        license="1234567890", issued="01/02/2020", licensePhoto="content://demo/license", passportPhoto="content://demo/passport")
    @Test fun acceptsComAndRu() { assertTrue(AuthValidation.email("user@example.com")); assertTrue(AuthValidation.email("user@example.ru")) }
    @Test fun rejectsInvalidEmail() { assertFalse(AuthValidation.email("user@")); assertFalse(AuthValidation.email("user@example.net")); assertFalse(AuthValidation.email("a b@example.com")) }
    @Test fun acceptsValidCredentials() { assertNull(AuthValidation.step1(valid)) }
    @Test fun rejectsShortPassword() { assertNotNull(AuthValidation.step1(valid.copy(password="123", confirm="123"))) }
    @Test fun rejectsMismatch() { assertEquals("Пароли не совпадают.", AuthValidation.step1(valid.copy(confirm="different"))) }
    @Test fun requiresConsent() { assertNotNull(AuthValidation.step1(valid.copy(consent=false))) }
    @Test fun middleNameOptional() { assertNull(AuthValidation.step2(valid.copy(middleName=""))) }
    @Test fun requiresGender() { assertNotNull(AuthValidation.step2(valid.copy(gender=""))) }
    @Test fun leapDateValid() { assertNotNull(AuthValidation.date("02/29/2000", "MM/dd/uuuu")) }
    @Test fun invalidDateRejected() { assertNull(AuthValidation.date("02/29/2001", "MM/dd/uuuu")); assertNull(AuthValidation.date("31/04/2020", "dd/MM/uuuu")) }
    @Test fun futureBirthRejected() { assertNotNull(AuthValidation.step2(valid.copy(birth="12/31/2999"))) }
    @Test fun acceptsDocumentsWithoutAvatar() { assertNull(AuthValidation.step3(valid.copy(avatar=null))) }
    @Test fun requiresTenCharacters() { assertNotNull(AuthValidation.step3(valid.copy(license="123"))) }
    @Test fun requiresBothDocuments() { assertNotNull(AuthValidation.step3(valid.copy(passportPhoto=null))); assertNotNull(AuthValidation.step3(valid.copy(licensePhoto=null))) }
    @Test fun futureIssueRejected() { assertNotNull(AuthValidation.step3(valid.copy(issued="31/12/2999"))) }
}
