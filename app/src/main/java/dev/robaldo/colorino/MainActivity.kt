package dev.robaldo.colorino

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import dev.robaldo.colorino.ui.theme.ColorinoTheme

@Composable
fun BluetoothPermissionRequest(onGranted: @Composable () -> Unit) {
    val context = LocalContext.current
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    var allGranted by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        allGranted = results.values.all { it }
    }

    LaunchedEffect(Unit) {
        val notGranted = permissions.any {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted) {
            launcher.launch(permissions)
        } else {
            allGranted = true
        }
    }

    if (allGranted) {
        onGranted()
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Bluetooth permissions are required")
        }
    }
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BluetoothPermissionRequest {
                val scanner = BLEScanner(this)
                ColorinoTheme {
                    val colours = remember { mutableStateListOf<ColorinoColour>() }

                    LaunchedEffect(Unit) {
                        scanner.start { colour ->
                            if(colours.isEmpty()) colours.add(colour)
                            else if (colours.last() != colour)
                                colours.add(0, colour)

                            if(colours.size > 50)
                                colours.remove(colours.last())
                        }
                    }

                    DisposableEffect(Unit) {
                        onDispose { scanner.stop() }
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text("Colorino")
                                }
                            )
                        }
                    ) { innerPadding ->
                        LazyColumn (
                            modifier = Modifier
                                .padding(12.dp)
                                .padding(innerPadding)
                                .fillMaxSize()
                        ) {
                            items(colours) { colour ->
                                Card (
                                    modifier = Modifier
                                        .fillMaxWidth()
                                ) {
                                    Column (
                                        modifier = Modifier.padding(12.dp),
                                    ) {
                                        Row {
                                            Icon(
                                                imageVector = Icons.Filled.Brightness1,
                                                contentDescription = null,
                                                tint = Color(colour.red, colour.green, colour.blue),
                                                modifier = Modifier.padding( end = 8.dp )
                                            )
                                            Text(
                                                colour.toNamedColor(),
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column (
                                                modifier = Modifier
                                                    .fillMaxWidth(0.5f)
                                                    .padding( end = 6.dp )
                                            ) {
                                                Text("Red", fontSize = 12.sp)
                                                Text(colour.red.toString())
                                                Text("Green", fontSize = 12.sp)
                                                Text(colour.green.toString())
                                                Text("Blue", fontSize = 12.sp)
                                                Text(colour.blue.toString())
                                            }
                                            Column (
                                                modifier = Modifier
                                                    .fillMaxWidth(0.5f)
                                                    .padding( end = 6.dp ),
                                                horizontalAlignment = Alignment.End
                                            ) {
                                                val hsl = colour.toHSL()

                                                Text("Hue", fontSize = 12.sp)
                                                Text(hsl.first.toString())
                                                Text("Saturation", fontSize = 12.sp)
                                                Text(hsl.second.toString())
                                                Text("Lightness", fontSize = 12.sp)
                                                Text(hsl.third.toString())
                                            }
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.End,
                                            modifier = Modifier.fillMaxWidth().padding( top = 12.dp )
                                        ) {
                                            TextButton(
                                                onClick = {

                                                }
                                            ) {
                                                Icon(Icons.Rounded.BookmarkAdd, contentDescription = null)
                                                Text("Bookmark")
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider( modifier = Modifier.padding( vertical = 8.dp ) )
                            }

                        }
                    }
                }
            }
        }
    }
}
