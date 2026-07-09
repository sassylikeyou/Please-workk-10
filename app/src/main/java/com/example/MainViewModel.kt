package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.server.NetworkUtils
import com.example.server.ServerManager
import com.example.server.ServerStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("server_prefs", Context.MODE_PRIVATE)

    private val _status = MutableStateFlow(ServerStatus.STOPPED)
    val status: StateFlow<ServerStatus> = _status.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _ipAddress = MutableStateFlow("Unknown")
    val ipAddress: StateFlow<String> = _ipAddress.asStateFlow()
    
    private val _memoryMb = MutableStateFlow(prefs.getInt("memory_mb", 600))
    val memoryMb: StateFlow<Int> = _memoryMb.asStateFlow()

    private val _activeTemplate = MutableStateFlow(com.example.server.template.TemplateRegistry.BEDROCK_CLOUDBURST_NUKKIT)
    val activeTemplate: StateFlow<com.example.server.template.ServerTemplate> = _activeTemplate.asStateFlow()

    private val serverManager = ServerManager(
        context = application.applicationContext,
        onLog = { newLog ->
            viewModelScope.launch {
                val currentLogs = _logs.value.toMutableList()
                currentLogs.add(newLog)
                if (currentLogs.size > 500) {
                    currentLogs.removeAt(0)
                }
                _logs.value = currentLogs
            }
        },
        onStatusChange = { newStatus ->
            _status.value = newStatus
        }
    )

    init {
        updateIpAddress()
        serverManager.checkIntegrity()
    }

    fun updateIpAddress() {
        _ipAddress.value = NetworkUtils.getLocalIpAddress()
    }
    
    fun setMemoryMb(memory: Int) {
        _memoryMb.value = memory
        prefs.edit().putInt("memory_mb", memory).apply()
    }

    fun setTemplate(template: com.example.server.template.ServerTemplate) {
        _activeTemplate.value = template
        serverManager.setTemplate(template)
    }

    fun startServer() {
        serverManager.startServer(_memoryMb.value)
    }

    fun stopServer() {
        serverManager.stopServer()
    }

    fun restartServer() {
        serverManager.stopServer()
        // Simple restart logic, wait a bit maybe, but for now just stop.
    }
    
    fun clearLogs() {
        _logs.value = emptyList()
    }
}
