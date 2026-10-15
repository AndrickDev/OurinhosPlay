package com.ourinhosplay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ourinhosplay.app.auth.AuthState
import com.ourinhosplay.app.auth.AuthViewModel
import com.ourinhosplay.app.ui.HomeShell
import com.ourinhosplay.app.ui.SplashScreen
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
                // A abertura animada dura ~3,2 s; só some quando a sessão também foi resolvida.
                var splashDone by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(3200)
                    splashDone = true
                }
                if (!splashDone || ui.state == AuthState.Loading) {
                    SplashScreen()
                    return@OurinhosPlayTheme
                }
                Scaffold(modifier = Modifier.fillMaxSize(), containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background) { inner ->
                    // Em tela larga (tablet, dobrável) o app fica numa coluna central de 600 dp.
                    Box(Modifier.padding(inner).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
                    Box(Modifier.widthIn(max = 600.dp).fillMaxSize()) {
                        when (val s = ui.state) {
                            AuthState.Loading -> Unit
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
}
