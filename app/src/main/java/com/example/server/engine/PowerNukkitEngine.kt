package com.example.server.engine

import android.content.Context
import com.example.server.ServerStatus

class PowerNukkitEngine(
    context: Context,
    onLog: (String) -> Unit,
    onStatusChange: (ServerStatus) -> Unit
) : BaseJavaEngine(context, onLog, onStatusChange) {
    override val serverJarUrl = "https://github.com/PowerNukkit/PowerNukkit/releases/download/v1.5.2.1-PN/powernukkit-1.5.2.1-PN-shaded.jar"
    override val serverFolderName = "powernukkit"
    override val serverJarName = "powernukkit.jar"
    override val serverEngineName = "PowerNukkit"
    override val minJavaVersion = 8
    override val maxJavaVersion = 21
}
