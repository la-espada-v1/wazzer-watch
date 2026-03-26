package com.laespada.wazzerwatch.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.laespada.wazzerwatch.presentation.theme.WazzerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp() {
    val context = LocalContext.current

    var isMeasuring by remember { mutableStateOf(false) }
    var heartRate by remember { mutableDoubleStateOf(0.0) }

    // 1. Define BOTH required permissions for Health Services
    val requiredPermissions = arrayOf(
        Manifest.permission.BODY_SENSORS,
        Manifest.permission.ACTIVITY_RECOGNITION
    )

    // 2. Check if both are already granted
    var hasPermissions by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    // 3. Use RequestMultiplePermissions to ask for both simultaneously
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissionsMap ->
            // Only proceed if the user grants ALL requested permissions
            hasPermissions = permissionsMap.values.all { it }
        }
    )

    val measureClient = remember { HealthServices.getClient(context).measureClient }

    val measureCallback = remember {
        object : MeasureCallback {
            override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) {}
            override fun onDataReceived(data: DataPointContainer) {
                val hrData = data.getData(DataType.HEART_RATE_BPM)
                if (hrData.isNotEmpty()) {
                    val currentHr = hrData.last().value
                    heartRate = currentHr
                    Log.d("PULSE", "Measured pulse: $currentHr")
                    sendDataToServer(currentHr)
                }
            }
        }
    }

    // 4. Added hasPermissions to the key, and wrapped API calls in try/catch blocks
    LaunchedEffect(isMeasuring, hasPermissions) {
        if (isMeasuring && hasPermissions) {
            try {
                measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, measureCallback)
            } catch (e: Exception) {
                Log.e("SENSOR_ERROR", "Failed to start sensor: ${e.message}")
                isMeasuring = false // Automatically toggle the button off if it fails
            }
        } else {
            try {
                // The try-catch prevents the app from crashing on startup when it tries to unregister an empty callback
                measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, measureCallback)
            } catch (e: Exception) {
                Log.e("SENSOR_ERROR", "Failed to stop sensor: ${e.message}")
            }
            heartRate = 0.0
        }
    }

    WazzerTheme {
        AppScaffold {
            ScreenScaffold {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (!hasPermissions) {
                        Text("Need permissions")
                        Button(onClick = { permissionLauncher.launch(requiredPermissions) }) {
                            Text("Give access")
                        }
                    } else {
                        Text(
                            text = if (heartRate > 0) "${heartRate.toInt()} BPM" else "waiting data",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { isMeasuring = !isMeasuring },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isMeasuring) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(if (isMeasuring) "STOP" else "START MEASURING")
                        }
                    }
                }
            }
        }
    }
}

fun sendDataToServer(heartRate: Double) {
    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        try {
            val url = URL("http://192.168.1.2:5000/measures")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; utf-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.doOutput = true

            val jsonParam = JSONObject()
            jsonParam.put("heart_rate", heartRate)

            val os = OutputStreamWriter(conn.outputStream)
            os.write(jsonParam.toString())
            os.flush()
            os.close()

            Log.d("NETWORK", "Successful request. Server returned code: ${conn.responseCode}")
            conn.disconnect()
        } catch (e: Exception) {
            Log.e("NETWORK", "Error with requesting: ${e.message}")
        }
    }
}