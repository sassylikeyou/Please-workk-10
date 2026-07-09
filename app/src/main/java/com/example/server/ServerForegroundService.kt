package com.example.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.NetworkInterface

class ServerForegroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var diagnosticJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }

        val notificationIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, "server_channel")
            .setContentTitle("Minecraft Server Running")
            .setContentText("The Bedrock server is active in the background.")
            .setSmallIcon(android.R.drawable.ic_menu_myplaces) // Placeholder icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(19132, notification)

        acquireWakeLock()
        startDiagnosticsLoop()

        return START_STICKY
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "BedrockBox::ServerWakeLock"
            )
        }
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire()
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    private fun startDiagnosticsLoop() {
        diagnosticJob?.cancel()
        diagnosticJob = scope.launch {
            while (true) {
                logDiagnostics()
                delay(30000)
            }
        }
    }

    private fun logDiagnostics() {
        Log.i("ServerDiagnostics", "--- 30s Server Diagnostic Report ---")
        Log.i("ServerDiagnostics", "Foreground Service: ACTIVE")
        Log.i("ServerDiagnostics", "WakeLock Held: ${wakeLock?.isHeld == true}")
        
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            var hasActiveInterface = false
            if (interfaces != null) {
                for (netInt in interfaces) {
                    if (netInt.isUp && !netInt.isLoopback) {
                        hasActiveInterface = true
                        val addresses = netInt.inetAddresses.toList().joinToString { it.hostAddress ?: "" }
                        Log.i("ServerDiagnostics", "Active Interface: ${netInt.name} - $addresses")
                    }
                }
            }
            if (!hasActiveInterface) {
                Log.w("ServerDiagnostics", "No active external network interfaces found!")
            }
        } catch (e: Exception) {
            Log.e("ServerDiagnostics", "Failed to get network interfaces: ${e.message}")
        }
        
        try {
            // Check if process is holding port 19132
            // We can't directly check the process's ports easily without root, but we can check if it's reachable or assume from java process.
            Log.i("ServerDiagnostics", "Java Process: Assuming running if we reached this point, Check Process stdout for UDP/RakNet")
        } catch (e: Exception) {}
        
        Log.i("ServerDiagnostics", "------------------------------------")
    }

    override fun onDestroy() {
        super.onDestroy()
        diagnosticJob?.cancel()
        releaseWakeLock()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "server_channel",
                "Minecraft Server Status",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
