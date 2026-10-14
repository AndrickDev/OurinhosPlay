package com.ourinhosplay.app.auth

import android.content.Context
import android.util.Log
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ourinhosplay.app.data.Profile
import com.ourinhosplay.app.data.ProfileRepository
import com.ourinhosplay.app.data.Supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data object NeedsName : AuthState
    data class SignedIn(val profile: Profile) : AuthState
}

data class AuthUi(
    val state: AuthState = AuthState.Loading,
    val busy: Boolean = false,
    val error: String? = null,
    val info: String? = null,
)

class AuthViewModel : ViewModel() {
    private val auth get() = Supabase.client.auth
    private val _ui = MutableStateFlow(AuthUi())
    val ui: StateFlow<AuthUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Initializing -> Unit
                    is SessionStatus.Authenticated -> loadProfile()
                    is SessionStatus.NotAuthenticated ->
                        _ui.update { it.copy(state = AuthState.SignedOut) }
                    is SessionStatus.RefreshFailure ->
                        _ui.update { it.copy(state = AuthState.SignedOut) }
                }
            }
        }
    }

    private suspend fun loadProfile() {
        runCatching { ProfileRepository.current() }
            .onSuccess { p ->
                val next = when {
                    p == null || p.displayName.isNullOrBlank() -> AuthState.NeedsName
                    else -> AuthState.SignedIn(p)
                }
                _ui.update { it.copy(state = next, busy = false) }
            }
            .onFailure { e ->
                Log.e(TAG, "loadProfile", e)
                _ui.update { it.copy(error = "Não consegui carregar seu perfil: ${e.message}", busy = false) }
            }
    }

    fun google(context: Context) = run("Google") { signInWithGoogle(context) }

    fun emailSignIn(email: String, password: String) = run("login") {
        auth.signInWith(Email) { this.email = email.trim(); this.password = password }
    }

    fun emailSignUp(email: String, password: String) = run("cadastro") {
        auth.signUpWith(Email) { this.email = email.trim(); this.password = password }
        _ui.update { it.copy(info = "Cadastro feito. Se o projeto exige confirmação, confirme o e-mail e entre.") }
    }

    fun saveName(name: String) {
        val clean = name.trim()
        if (clean.length < 2) {
            _ui.update { it.copy(error = "Use pelo menos 2 letras.") }
            return
        }
        run("salvar nome") {
            ProfileRepository.setDisplayName(clean)
            loadProfile()
        }
    }

    fun signOut() = run("sair") { auth.signOut() }

    private fun run(what: String, block: suspend () -> Unit) {
        _ui.update { it.copy(busy = true, error = null, info = null) }
        viewModelScope.launch {
            runCatching { block() }
                .onFailure { e ->
                    Log.e(TAG, what, e)
                    _ui.update { it.copy(error = friendly(e)) }
                }
            _ui.update { it.copy(busy = false) }
        }
    }

    private fun friendly(e: Throwable): String = when (e) {
        is GetCredentialCancellationException -> "Login cancelado."
        is NoCredentialException -> "Nenhuma conta Google no aparelho. Adicione uma conta em Configurações."
        else -> e.message ?: e::class.java.simpleName
    }

    private companion object {
        const val TAG = "OurinhosPlay"
    }
}
