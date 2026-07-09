package com.example.server.engine

import android.content.Context
import com.example.server.ServerStatus

class NukkitEngine(
    context: Context,
    onLog: (String) -> Unit,
    onStatusChange: (ServerStatus) -> Unit
) : BaseJavaEngine(context, onLog, onStatusChange) {
    override val serverJarUrl = "https://github.com/PetteriM1/NukkitPetteriM1Edition/releases/download/4437/Nukkit-PM1E.jar"
    override val serverFolderName = "nukkit"
    override val serverJarName = "nukkit.jar"
    override val serverEngineName = "Nukkit"
    override val minJavaVersion = 8
    override val maxJavaVersion = 21
}
