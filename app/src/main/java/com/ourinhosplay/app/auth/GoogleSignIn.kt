package com.ourinhosplay.app.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.ourinhosplay.app.BuildConfig
import com.ourinhosplay.app.data.Supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import java.security.MessageDigest
import java.util.UUID

/** Abre o seletor de contas do Google e troca o ID token por sessão no Supabase. */
suspend fun signInWithGoogle(context: Context) {
    val rawNonce = UUID.randomUUID().toString()
    val hashedNonce = MessageDigest.getInstance("SHA-256")
        .digest(rawNonce.toByteArray())
        .joinToString("") { "%02x".format(it) }

    val option = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        .setNonce(hashedNonce)
        .build()

    val result = CredentialManager.create(context).getCredential(
        context,
        GetCredentialRequest.Builder().addCredentialOption(option).build(),
    )
    val credential = result.credential
    require(
        credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) { "Credencial inesperada: ${credential.type}" }

    val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
    Supabase.client.auth.signInWith(IDToken) {
        idToken = token
        provider = Google
        nonce = rawNonce
    }
}
