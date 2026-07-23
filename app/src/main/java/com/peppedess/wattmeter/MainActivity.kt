package com.peppedess.wattmeter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peppedess.wattmeter.service.MonitorService
import com.peppedess.wattmeter.ui.HomeScreen
import com.peppedess.wattmeter.ui.SettingsDialog
import com.peppedess.wattmeter.ui.theme.WattMeterTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            MonitorService.start(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = viewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()

            WattMeterTheme(dynamicColor = state.dynamicColor) {
                AppRoot(
                    viewModel = viewModel,
                    state = state,
                    onRequestService = { requestServiceStart() }
                )
            }
        }
    }

    private fun requestServiceStart() {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        MonitorService.start(this)
    }
}

@Composable
private fun AppRoot(
    viewModel: MainViewModel,
    state: UiState,
    onRequestService: () -> Unit
) {
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.setServiceRunning(MonitorService.isRunning(context))
            delay(2000L)
        }
    }

    HomeScreen(
        state = state,
        onOpenSettings = { showSettings = true },
        onToggleService = {
            if (MonitorService.isRunning(context)) {
                MonitorService.stop(context)
                viewModel.setServiceRunning(false)
            } else {
                onRequestService()
                viewModel.setServiceRunning(true)
            }
        },
        onResetSession = { viewModel.resetSession() }
    )

    if (showSettings) {
        SettingsDialog(
            currentUnit = state.currentUnit,
            onlyWhileCharging = state.onlyWhileCharging,
            dynamicColor = state.dynamicColor,
            onUnitChange = { viewModel.setCurrentUnit(it) },
            onOnlyWhileChargingChange = { viewModel.setOnlyWhileCharging(it) },
            onDynamicColorChange = { viewModel.setDynamicColor(it) },
            onResetRecords = { viewModel.resetRecords() },
            onDismiss = { showSettings = false }
        )
    }
}
