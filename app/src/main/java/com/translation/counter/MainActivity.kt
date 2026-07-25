package com.translation.counter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.translation.counter.data.CounterRoom
import com.translation.counter.data.DeviceRole
import com.translation.counter.ui.GuestCounterScreen
import com.translation.counter.ui.InitialSetupScreen
import com.translation.counter.ui.MainViewModel
import com.translation.counter.ui.StaffCounterScreen
import com.translation.counter.ui.theme.CounterTranslationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (!recordAudioGranted) {
            Toast.makeText(this, "음성 통역을 위해 마이크 권한이 필요합니다.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        setContent {
            CounterTranslationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val selectedRoom by viewModel.selectedRoom.collectAsState()
                    val selectedRole by viewModel.selectedRole.collectAsState()
                    val geminiApiKey by viewModel.geminiApiKey.collectAsState()

                    // Hardware Back Button Handler
                    BackHandler(enabled = selectedRoom != null) {
                        viewModel.resetToSetup()
                    }

                    if (selectedRoom == null || selectedRole == null) {
                        InitialSetupScreen(
                            geminiApiKey = geminiApiKey,
                            onSaveApiKey = { newKey ->
                                viewModel.saveGeminiApiKey(newKey)
                            },
                            onSetupComplete = { room, role ->
                                viewModel.selectRoomAndRole(room, role)
                            }
                        )
                    } else {
                        when (selectedRole) {
                            DeviceRole.STAFF -> StaffCounterScreen(
                                room = selectedRoom!!,
                                viewModel = viewModel
                            )
                            DeviceRole.GUEST -> GuestCounterScreen(
                                room = selectedRoom!!,
                                viewModel = viewModel
                            )
                            null -> {}
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO
        )
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
}
