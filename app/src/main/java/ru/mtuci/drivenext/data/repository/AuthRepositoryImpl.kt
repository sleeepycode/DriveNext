package ru.mtuci.drivenext.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import ru.mtuci.drivenext.domain.model.SessionState
import ru.mtuci.drivenext.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val client: SupabaseClient
) : AuthRepository {

    override suspend fun checkSession(): SessionState {

        return withTimeoutOrNull(10_000L) {

            try {

                val auth = client.auth

                auth.awaitInitialization()

                when (auth.sessionStatus.value) {

                    is SessionStatus.Authenticated -> {

                        auth.retrieveUserForCurrentSession()

                        SessionState.AUTHENTICATED
                    }

                    is SessionStatus.NotAuthenticated ->
                        SessionState.NOT_AUTHENTICATED

                    else ->
                        SessionState.UNAVAILABLE
                }

            } catch (e: CancellationException) {

                throw e

            } catch (e: RestException) {

                when (e.statusCode) {
                    401, 403 -> SessionState.NOT_AUTHENTICATED
                    else -> SessionState.UNAVAILABLE
                }

            } catch (e: Exception) {

                SessionState.UNAVAILABLE
            }

        } ?: SessionState.UNAVAILABLE
    }
}