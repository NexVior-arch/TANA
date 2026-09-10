package com.tana

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.tana.ui.navigation.NavigationRoot
import com.tana.ui.theme.TanaTheme
import com.tana.ui.viewmodel.FinanceViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FinanceViewModel by viewModels()
    private var pendingWidgetAction by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate() and before setContentView/setContent.
        // Replaces the plain black "generic icon in the middle" screen Android shows
        // by default with our own splash (warm charcoal background + TANA brand mark
        // icon), configured in Theme.Tana.Splash in themes.xml.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep the splash visible until the app's initial data (savings goals,
        // transactions, preferences) has loaded, so it never disappears into a
        // half-empty screen that then pops in content a moment later.
        splashScreen.setKeepOnScreenCondition { !viewModel.isInitialDataLoaded.value }

        pendingWidgetAction = intent?.action

        setContent {
            TanaTheme {
                NavigationRoot(
                    viewModel = viewModel,
                    initialWidgetAction = pendingWidgetAction,
                    onWidgetActionHandled = { pendingWidgetAction = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingWidgetAction = intent.action
    }
}
