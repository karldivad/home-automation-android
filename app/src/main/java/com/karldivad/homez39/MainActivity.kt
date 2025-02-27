package com.karldivad.homez39

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.concurrent.thread
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.net.Uri

class MainActivity : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        val appLinkIntent: Intent = intent
        val appLinkAction: String? = appLinkIntent.action
        var triggerDoor = appLinkAction?.contains("OPEN_APP_FEATURE") == true

        intent?.data?.let { uri ->
            if (uri.host == "openMainDoor") {
                triggerDoor = true;
            }
        }

        setContent {
            MyApp(sharedPreferences, triggerDoor)
        }

    }
}

@Composable
fun MyApp(sharedPreferences: SharedPreferences, triggerDoor: Boolean) {
    var url by remember { mutableStateOf(sharedPreferences.getString("saved_url", "") ?: "") }
    var response by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var triggerClick by remember { mutableStateOf(false) }

    LaunchedEffect(triggerDoor) {
        if (triggerDoor) {
            triggerClick = true
        }
    }

    val rainbowColorsBrush = remember {
        Brush.sweepGradient(
            listOf(
                Color(0xFF9575CD),
                Color(0xFFBA68C8),
                Color(0xFFE57373),
                Color(0xFFFFB74D),
                Color(0xFFFFF176),
                Color(0xFFAED581),
                Color(0xFF4DD0E1),
                Color(0xFF9575CD)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center

    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(response, color = Color.White)
            Image(
                painter = painterResource(id = R.drawable.door),
                contentDescription = "Door",
                contentScale = ContentScale.Inside,
                modifier = Modifier
                    .size(150.dp)
                    .border(
                        BorderStroke(4.dp, rainbowColorsBrush),
                        CircleShape
                    )
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OpenButton(url, {response = it}, triggerClick, { triggerClick = false } )
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
            Button(onClick = { showDialog = true }, colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF000000),
                contentColor = Color.Black,
            ), modifier = Modifier.padding(16.dp)) {
                Text("Editar")
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Editar URL") },
            text = {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Ingresar URL") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    sharedPreferences.edit().putString("saved_url", url).apply()
                    showDialog = false
                }) {
                    Text("Grabar")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

fun makeGetRequest(url: String, onResult: (String) -> Unit) {
    thread {
        try {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "No response"
            println("Response: ${responseBody}")
            onResult(responseBody)
        } catch (e: Exception) {
            println("Error: ${e.message}")
            onResult("Error: ${e.message}")
        }
    }
}

@Composable
fun OpenButton(url: String, onResult: (String) -> Unit, triggerClick: Boolean, onTriggerHandled: () -> Unit) {
    var enabled by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // React to triggerClick state
    LaunchedEffect(triggerClick) {
        if (triggerClick && enabled) {
            makeGetRequest(url, onResult)
            enabled = false
            scope.launch {
                delay(5000)
                enabled = true
            }
            onTriggerHandled()  // Reset trigger after handling
        }
    }

    Button(
        onClick = {
            makeGetRequest(url, onResult)
            enabled = false
            scope.launch {
                delay(5000)
                enabled = true
            }
        },
        enabled = enabled,
        modifier = Modifier.padding(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2196F3),
            disabledContainerColor = Color(0xFFBBDEFB),
            contentColor = Color.White,
            disabledContentColor = Color(0xFF757575)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 4.dp,
            disabledElevation = 0.dp
        ),
        border = BorderStroke(
            width = if (enabled) 2.dp else 1.dp,
            color = if (enabled) Color(0xFF1976D2) else Color(0xFFBDBDBD)
        ),
        shape = RoundedCornerShape(if (enabled) 8.dp else 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!enabled) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "Wait",
                    modifier = Modifier.size(18.dp).graphicsLayer {
                        alpha = if (enabled) 1f else 0.5f
                        rotationZ = if (enabled) 0f else -2f
                    }
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(if (enabled) "Abrir puerta" else "Espera 5s")
        }
    }
}