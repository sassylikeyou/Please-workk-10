package com.example.server.health

import com.example.server.ServerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ServerHealthMonitor(
    private val onLog: (String) -> Unit,
    private val onStatusChange: (ServerStatus) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var verificationJob: Job? = null
    
    @Volatile
    private var currentStatus: ServerStatus = ServerStatus.STOPPED

    fun setStatus(status: ServerStatus) {
        if (currentStatus == status) return
        currentStatus = status
        onStatusChange(status)
    }

    fun analyzeLogLine(line: String) {
        if (line.contains("libutil.so.1") || line.contains("jlinenative") || line.contains("Advanced terminal features unavailable")) {
            // These are Android compatibility warnings, ignore them.
        }
        
        if (currentStatus == ServerStatus.STARTING) {
            if (ServerStartupPatterns.isSuccess(line)) {
                // Potential success, wait a short verification period to see if it crashes immediately
                if (verificationJob == null || verificationJob?.isActive != true) {
                    verificationJob = scope.launch {
                        delay(1000) // 1 second verification
                        if (currentStatus == ServerStatus.STARTING) {
                            onLog("SERVER READY STATE: Online!")
                            setStatus(ServerStatus.RUNNING)
                        }
                    }
                }
            }
        }
    }
    
    fun onProcessExit(exitCode: Int) {
        verificationJob?.cancel()
        if (exitCode != 0 && currentStatus != ServerStatus.STOPPED && currentStatus != ServerStatus.STOPPING) {
            setStatus(ServerStatus.CRASHED)
        } else if (currentStatus != ServerStatus.CRASHED) {
            setStatus(ServerStatus.STOPPED)
        }
    }

    fun onProcessStart() {
        setStatus(ServerStatus.STARTING)
    }
}
