package ru.mtuci.drivenext.data.repository

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.RestException
import org.json.JSONObject
import ru.mtuci.drivenext.data.remote.ProfileRemote
import ru.mtuci.drivenext.domain.model.AuthValidation
import ru.mtuci.drivenext.domain.model.RegistrationDraft
import ru.mtuci.drivenext.domain.repository.AccountRepository

class AccountRepositoryImpl(private val client: SupabaseClient, context: Context) : AccountRepository {
    private val remote = ProfileRemote(context.applicationContext)
    private val auth get() = client.auth
    private fun token() = requireNotNull(auth.currentSessionOrNull()) { "Войдите в аккаунт повторно." }.accessToken
    private suspend fun isComplete(): Boolean {
        val user = auth.retrieveUserForCurrentSession()
        return remote.complete(user.id, token())
    }
    override suspend fun login(email: String, password: String): Boolean {
        auth.awaitInitialization()
        auth.signInWith(Email) { this.email = email; this.password = password }
        return isComplete()
    }
    override suspend fun register(draft: RegistrationDraft) {
        auth.awaitInitialization()
        // Retry after partial registration: reuse only the authenticated account for this email.
        val existing = auth.currentUserOrNull()
        if (existing?.email?.equals(draft.email.trim(), ignoreCase = true) != true || auth.currentSessionOrNull() == null) {
            try {
                auth.signUpWith(Email) { email = draft.email.trim(); password = draft.password }
            } catch (e: RestException) {
                if (e.statusCode !in listOf(400, 422, 409)) throw e
                // An existing account may be resumed only with its valid password.
                auth.signInWith(Email) { email = draft.email.trim(); password = draft.password }
            }
        }
        require(auth.currentSessionOrNull() != null) { "Подтвердите электронную почту, затем войдите и завершите регистрацию." }
        val user = auth.retrieveUserForCurrentSession()
        require(user.email?.equals(draft.email.trim(), ignoreCase = true) == true) { "Войдите в нужный аккаунт." }
        if (remote.complete(user.id, token())) {
            throw IllegalArgumentException("Этот аккаунт уже зарегистрирован. Войдите в него.")
        }
        val avatar = draft.avatar?.let { remote.upload(user.id, "avatar", it, token()) }
        val license = remote.upload(user.id, "license", requireNotNull(draft.licensePhoto), token())
        val passport = remote.upload(user.id, "passport", requireNotNull(draft.passportPhoto), token())
        val profile = JSONObject().apply {
            put("id", user.id); put("email", user.email ?: draft.email.trim()); put("first_name", draft.firstName.trim())
            put("last_name", draft.lastName.trim()); put("middle_name", draft.middleName.trim())
            put("birth_date", AuthValidation.date(draft.birth, "MM/dd/uuuu").toString())
            put("gender", draft.gender); put("license_number", draft.license.trim())
            put("license_issued", AuthValidation.date(draft.issued, "dd/MM/uuuu").toString())
            put("avatar_path", avatar ?: JSONObject.NULL); put("license_path", license); put("passport_path", passport)
            put("consent", true); put("registration_complete", true)
        }
        remote.save(profile, token())
    }
    override suspend fun startGoogle() { auth.awaitInitialization(); auth.signInWith(Google) }
    override suspend fun finishGoogle(code: String): Boolean {
        auth.awaitInitialization(); auth.exchangeCodeForSession(code)
        return isComplete()
    }
    override suspend fun resetPassword(email: String) { auth.resetPasswordForEmail(email, redirectUrl = "drivenext://auth/recovery") }
    override suspend fun updatePassword(password: String): Boolean { auth.updateUser { this.password = password }; return isComplete() }
}
