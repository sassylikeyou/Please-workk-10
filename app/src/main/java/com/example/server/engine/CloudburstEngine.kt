package com.example.server.engine

import android.content.Context
import com.example.server.ServerStatus

class CloudburstEngine(
    context: Context,
    onLog: (String) -> Unit,
    onStatusChange: (ServerStatus) -> Unit
) : BaseJavaEngine(context, onLog, onStatusChange) {
    override val serverJarUrl = "https://repo.opencollab.dev/maven-snapshots/cn/nukkit/nukkit/1.0-SNAPSHOT/nukkit-1.0-20260616.184029-1239.jar"
    override val serverFolderName = "cloudburst"
    override val serverJarName = "cloudburst.jar"
    override val serverEngineName = "Cloudburst"
    override val minJavaVersion = 21
    override val maxJavaVersion = 21
}
