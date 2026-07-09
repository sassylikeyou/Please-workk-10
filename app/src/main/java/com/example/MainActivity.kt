package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.server.ServerStatus
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Settings, contentDescription = "Server", tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("BedrockBox", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }
                ) { innerPadding ->
                    ServerScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun ServerScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val status by viewModel.status.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val ipAddress by viewModel.ipAddress.collectAsState()

    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Server Status", color = ServerTextSecondary, fontWeight = FontWeight.Medium)
                    StatusBadge(status)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Connect via LAN", color = ServerTextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(ipAddress, style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text("Port 19132", color = ServerTextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.startServer() },
                enabled = status == ServerStatus.STOPPED || status == ServerStatus.ERROR || status == ServerStatus.CRASHED,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ServerSuccess)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Start", modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Start", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Button(
                onClick = { viewModel.stopServer() },
                enabled = status == ServerStatus.RUNNING || status == ServerStatus.STARTING,
                colors = ButtonDefaults.buttonColors(containerColor = ServerError),
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Stop", modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Stop", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        Button(
            onClick = { viewModel.restartServer() },
            enabled = status == ServerStatus.RUNNING,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = "Restart")
            Spacer(Modifier.width(8.dp))
            Text("Restart Server", fontWeight = FontWeight.Medium)
        }

        val memoryMb by viewModel.memoryMb.collectAsState()
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Allocated Memory: ${memoryMb} MB", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Slider(
                value = memoryMb.toFloat(),
                onValueChange = { viewModel.setMemoryMb(it.toInt()) },
                valueRange = 256f..2048f,
                enabled = status == ServerStatus.STOPPED || status == ServerStatus.ERROR || status == ServerStatus.CRASHED
            )
        }

        val activeTemplate by viewModel.activeTemplate.collectAsState()

        var engineExpanded by remember { mutableStateOf(false) }
        var showAllEngines by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { 
                    engineExpanded = true
                    showAllEngines = false
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = status == ServerStatus.STOPPED || status == ServerStatus.ERROR || status == ServerStatus.CRASHED,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Engine: ${activeTemplate.name}", fontSize = 14.sp)
            }
            DropdownMenu(expanded = engineExpanded, onDismissRequest = { engineExpanded = false }) {
                val topTemplates = listOf(
                    com.example.server.template.TemplateRegistry.BEDROCK_NUKKIT,
                    com.example.server.template.TemplateRegistry.BEDROCK_POWER_NUKKIT_X,
                    com.example.server.template.TemplateRegistry.BEDROCK_CLOUDBURST_NUKKIT
                )
                val otherTemplates = com.example.server.template.TemplateRegistry.ALL_TEMPLATES.filter { it !in topTemplates }

                topTemplates.forEach { template ->
                    DropdownMenuItem(text = { Text(template.name) }, onClick = { 
                        viewModel.setTemplate(template)
                        engineExpanded = false
                    })
                }

                if (!showAllEngines) {
                    DropdownMenuItem(text = { Text("See more...", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }, onClick = { 
                        showAllEngines = true
                    })
                } else {
                    HorizontalDivider()
                    otherTemplates.forEach { template ->
                        DropdownMenuItem(text = { Text(template.name) }, onClick = { 
                            viewModel.setTemplate(template)
                            engineExpanded = false
                        })
                    }
                }
            }
        }

        // Console Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Live Console",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row {
                val clipboardManager = LocalClipboardManager.current
                IconButton(onClick = {
                    clipboardManager.setText(AnnotatedString(logs.joinToString("\n")))
                }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Logs", tint = ServerTextSecondary)
                }
                IconButton(onClick = { viewModel.clearLogs() }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Clear Logs", tint = ServerTextSecondary)
                }
            }
        }

        // Live Log Console
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ConsoleBackground)
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            if (logs.isEmpty()) {
                 Text("Ready.", color = ServerTextSecondary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(logs) { log ->
                    Text(
                        text = log,
                        color = ConsoleText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: ServerStatus) {
    val targetColor = when (status) {
        ServerStatus.STOPPED -> ServerTextSecondary
        ServerStatus.STARTING -> ServerWarning
        ServerStatus.RUNNING -> ServerSuccess
        ServerStatus.STOPPING -> ServerWarning
        ServerStatus.CRASHED -> ServerError
        ServerStatus.ERROR -> ServerError
    }
    
    val targetText = when (status) {
        ServerStatus.STOPPED -> "STOPPED"
        ServerStatus.STARTING -> "STARTING"
        ServerStatus.RUNNING -> "ONLINE"
        ServerStatus.STOPPING -> "STOPPING"
        ServerStatus.CRASHED -> "CRASHED"
        ServerStatus.ERROR -> "ERROR"
    }

    val color by animateColorAsState(targetValue = targetColor, label = "StatusColor")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = targetText,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp
        )
    }
}
