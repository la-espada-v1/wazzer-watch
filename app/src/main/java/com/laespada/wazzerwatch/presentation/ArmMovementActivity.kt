package com.laespada.wazzerwatch.presentation

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.laespada.wazzerwatch.presentation.theme.WazzerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ArmMovementActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EmergencyScreen()
        }
    }
}

@Composable
fun EmergencyScreen() {
    val activity = LocalContext.current as Activity
    val context = LocalContext.current
    var countdown by remember { mutableIntStateOf(15) }
    var emergencySent by remember { mutableStateOf(false) }


    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_ALARM, 100) }

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            vibrator.cancel()
            toneGenerator.release()
        }
    }

    LaunchedEffect(Unit) {
        while (countdown > 0) {

            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }

            delay(1000)
            countdown--
        }

        if (countdown == 0 && !emergencySent) {
            sendEmergencyNotification()
            emergencySent = true
        }
    }

    WazzerTheme {
        AppScaffold {
            ScreenScaffold {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF8B0000))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (emergencySent) {
                            Text("ALERT SENT!", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { activity.finish() }) {
                                Text("Close")
                            }
                        } else {
                            Text("FALL DETECTED!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("SMS in: $countdown sec", color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { activity.finish() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                            ) {
                                Text("I'M OKAY", color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun sendEmergencyNotification() {
    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        try {
            val url = URL("http://192.168.1.2:5000/notify")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; utf-8")
            conn.doOutput = true
            val jsonParam = JSONObject().apply {
                put("alert", "FALL_DETECTED")
                put("message", "The person is unconscious!")
            }
            val os = OutputStreamWriter(conn.outputStream)
            os.write(jsonParam.toString())
            os.flush()
            os.close()
            Log.d("EMERGENCY", "Signal: ${conn.responseCode}")
            conn.disconnect()
        } catch (e: Exception) {
            Log.e("EMERGENCY", "Error: ${e.message}")
        }
    }
}