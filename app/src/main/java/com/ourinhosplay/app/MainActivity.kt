package com.ourinhosplay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ourinhosplay.app.auth.AuthState
import com.ourinhosplay.app.auth.AuthViewModel
import com.ourinhosplay.app.ui.HomeShell
import com.ourinhosplay.app.ui.LoadingScreen
import com.ourinhosplay.app.ui.LoginScreen
import com.ourinhosplay.app.ui.NameScreen
import com.ourinhosplay.app.ui.OurinhosPlayTheme

class MainActivity : ComponentActivity() {
    private val vm: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            OurinhosPlayTheme {
                val ui by vm.ui.collectAsState()
                Scaffold(modifier = Modifier.fillMaxSize()) { inner ->
                    Box(Modifier.padding(inner)) {
                        when (val s = ui.state) {
                            AuthState.Loading -> LoadingScreen()
                            AuthState.SignedOut -> LoginScreen(
                                ui = ui,
                                onGoogle = { vm.google(this@MainActivity) },
                                onEmailSignIn = vm::emailSignIn,
                                onEmailSignUp = vm::emailSignUp,
                            )
                            AuthState.NeedsName -> NameScreen(ui, vm::saveName, vm::signOut)
                            is AuthState.SignedIn -> HomeShell(
                                displayName = s.profile.fullName,
                                onSignOut = vm::signOut,
                            )
                        }
                    }
                }
            }
        }
    }
}
