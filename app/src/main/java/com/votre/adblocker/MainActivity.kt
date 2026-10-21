package com.votre.adblocker

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {
    private val _isVpnActive = MutableStateFlow(false)
    val isVpnActive: StateFlow<Boolean> = _isVpnActive.asStateFlow()

    private val _blockedCount = MutableStateFlow(0)
    val blockedCount: StateFlow<Int> = _blockedCount.asStateFlow()

    fun toggleVpn(enabled: Boolean) {
        _isVpnActive.value = enabled
    }
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainScreen(
                    viewModel = viewModel,
                    onRequestVpnPermission = {
                        val intent = VpnService.prepare(this)
                        if (intent != null) {
                            vpnPermissionLauncher.launch(intent)
                        } else {
                            startVpnService()
                        }
                    },
                    onStopVpn = {
                        stopVpnService()
                    }
                )
            }
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, AdBlockVpnService::class.java)
        startService(intent)
        viewModel.toggleVpn(true)
    }

    private fun stopVpnService() {
        val intent = Intent(this, AdBlockVpnService::class.java)
        stopService(intent)
        viewModel.toggleVpn(false)
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRequestVpnPermission: () -> Unit,
    onStopVpn: () -> Unit
) {
    val isVpnActive by viewModel.isVpnActive.collectAsState()
    val blockedCount by viewModel.blockedCount.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Mon AdBlocker",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(200.dp)
                .background(
                    color = if (isVpnActive) Color(0xFF4CAF50) else Color(0xFFE53935),
                    shape = CircleShape
                )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "PROTECTION",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = if (isVpnActive) "ACTIVÉE" else "DÉSACTIVÉE",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Publicités & Traqueurs bloqués",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$blockedCount",
                    color = Color(0xFF64B5F6),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Activer le bloqueur",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Switch(
                checked = isVpnActive,
                onCheckedChange = { checked ->
                    if (checked) {
                        onRequestVpnPermission()
                    } else {
                        onStopVpn()
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF4CAF50)
                )
            )
        }
    }
}
