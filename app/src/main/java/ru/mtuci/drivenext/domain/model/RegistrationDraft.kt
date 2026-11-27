package ru.mtuci.drivenext.domain.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

// Passwords and document locations are kept in memory only.
data class RegistrationDraft(
    val email: String = "", val password: String = "", val confirm: String = "",
    val consent: Boolean = false, val lastName: String = "", val firstName: String = "",
    val middleName: String = "", val birth: String = "", val gender: String = "",
    val license: String = "", val issued: String = "", val avatar: String? = null,
    val licensePhoto: String? = null, val passportPhoto: String? = null
)

object AuthValidation {
    private val emailPattern = Regex("^[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)*\\.(com|ru)$", RegexOption.IGNORE_CASE)
    fun email(value: String) = emailPattern.matches(value.trim())
    fun date(value: String, pattern: String): LocalDate? = try {
        LocalDate.parse(value, DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.STRICT))
    } catch (_: Exception) { null }
    fun step1(d: RegistrationDraft): String? = when {
        !email(d.email) -> "Введите корректный адрес электронной почты."
        d.password.length < 6 -> "Пароль должен содержать не менее 6 символов."
        d.password != d.confirm -> "Пароли не совпадают."
        !d.consent -> "Необходимо согласиться с условиями обслуживания и политикой конфиденциальности."
        else -> null
    }
    fun step2(d: RegistrationDraft): String? = when {
        d.firstName.isBlank() || d.lastName.isBlank() || d.birth.isBlank() || d.gender !in listOf("male", "female") -> "Пожалуйста, заполните все обязательные поля."
        date(d.birth, "MM/dd/uuuu")?.let { !it.isAfter(LocalDate.now()) && it.year >= 1900 } != true -> "Введите корректную дату рождения."
        else -> null
    }
    fun step3(d: RegistrationDraft): String? = when {
        d.license.isBlank() || d.issued.isBlank() -> "Пожалуйста, заполните все обязательные поля."
        d.license.length != 10 || d.license.any { it.isWhitespace() } -> "Номер водительского удостоверения должен содержать 10 символов."
        date(d.issued, "dd/MM/uuuu")?.let { !it.isAfter(LocalDate.now()) && it.year >= 1900 } != true -> "Введите корректную дату выдачи."
        d.licensePhoto == null || d.passportPhoto == null -> "Пожалуйста, загрузите все необходимые фото."
        else -> null
    }
}
