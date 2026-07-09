package com.example.server.engine

import android.content.Context
import com.example.server.ServerStatus

class PowerNukkitXEngine(
    context: Context,
    onLog: (String) -> Unit,
    onStatusChange: (ServerStatus) -> Unit
) : BaseJavaEngine(context, onLog, onStatusChange) {
    override val serverJarUrl = "https://github.com/PowerNukkitX/PowerNukkitX/releases/download/2.0.0/powernukkitx.jar"
    override val serverFolderName = "powernukkitx"
    override val serverJarName = "powernukkitx.jar"
    override val serverEngineName = "PowerNukkitX"
    override val minJavaVersion = 21
    override val maxJavaVersion = 21
}
