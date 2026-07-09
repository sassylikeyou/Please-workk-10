package com.example.server

import android.content.Context
import com.example.server.engine.ServerEngine
import com.example.server.template.ServerTemplate
import com.example.server.template.TemplateRegistry
import java.io.File
import com.example.server.engine.NukkitEngine

class ServerManager(
    private val context: Context,
    private val onLog: (String) -> Unit,
    private val onStatusChange: (ServerStatus) -> Unit
) {
    private var currentEngine: ServerEngine? = null
    var activeTemplate: ServerTemplate = TemplateRegistry.BEDROCK_CLOUDBURST_NUKKIT // Default
    
    // We keep checkIntegrity to extract files, since Downloader is used for Bedrock setup currently.
    fun checkIntegrity() {
        // Just keeping the old signature, maybe we can delegate to NukkitEngine or generic.
        // Actually Downloader was used in the old ServerManager. Let's delegate.
        // Wait, old ServerManager has checkIntegrity which calls Downloader. We should probably keep that.
        // Let's create an instance of NukkitEngine just for checkIntegrity for now to not break anything.
        val engine = getEngine()
        if (engine is com.example.server.engine.BaseJavaEngine) engine.checkIntegrity()
    }
    
    fun setTemplate(template: ServerTemplate) {
        if (currentEngine?.getStatus() == ServerStatus.RUNNING) {
            onLog("Cannot change template while server is running.")
            return
        }
        activeTemplate = template
        currentEngine = ServerFactory.createEngine(context, template, onLog, onStatusChange)
        onLog("Switched to template: \${template.name}")
    }

    private fun getEngine(): ServerEngine {
        if (currentEngine == null) {
            currentEngine = ServerFactory.createEngine(context, activeTemplate, onLog, onStatusChange)
        }
        return currentEngine!!
    }

    fun startServer(memoryMb: Int = 600) {
        getEngine().startServer(memoryMb)
    }

    fun stopServer() {
        currentEngine?.stopServer()
    }

    fun restartServer() {
        currentEngine?.restartServer()
    }
    
    fun sendCommand(command: String) {
        currentEngine?.sendCommand(command)
    }
}
