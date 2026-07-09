package com.example.server.engine

import com.example.server.ServerStatus

interface ServerEngine {
    fun startServer(memoryMb: Int)
    fun stopServer()
    fun restartServer()
    fun sendCommand(command: String)
    fun getStatus(): ServerStatus
    fun installPlugin(url: String, fileName: String)
    fun backupWorld()
}
