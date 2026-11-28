package ru.mtuci.drivenext.domain.model

data class Car(
    val id: Long, val brand: String, val model: String, val dailyPrice: Int,
    val transmission: String, val fuel: String, val imageKey: String
)

data class UserProfile(
    val id: String, val email: String, val firstName: String, val lastName: String,
    val gender: String, val provider: String, val avatarPath: String?, val createdAt: String
) {
    val displayName: String get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { email }
}

object CatalogQuery {
    fun normalize(text: String): String = text.trim().take(100).replace(Regex("\\s+"), " ")
}
