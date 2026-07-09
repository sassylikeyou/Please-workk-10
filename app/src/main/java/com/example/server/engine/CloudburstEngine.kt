package com.example.server.engine

import android.content.Context
import com.example.server.ServerStatus

class CloudburstEngine(
    context: Context,
    onLog: (String) -> Unit,
    onStatusChange: (ServerStatus) -> Unit
) : BaseJavaEngine(context, onLog, onStatusChange) {
    override val serverJarUrl = "https://github.com/PetteriM1/NukkitPetteriM1Edition/releases/download/4437/Nukkit-PM1E.jar"
    override val serverFolderName = "cloudburst"
    override val serverJarName = "cloudburst.jar"
    override val serverEngineName = "Cloudburst"
    override val minJavaVersion = 21
    override val maxJavaVersion = 21
}
