package com.example.server.health

object ServerStartupPatterns {
    val SUCCESS_PATTERNS = listOf(
        "Done (",
        "!) For help, type",
        "Server started",
        "Listening on",
        "Started successfully",
        "For help, type",
        "Loading PowerNukkitX",
        "Done",
        "Ready",
        "Opening server on",
        "SERVER READY STATE: Online!"
    )

    val ERROR_PATTERNS = listOf(
        "Exception in thread",
        "java.lang.ExceptionInInitializerError",
        "java.lang.RuntimeException:",
        "Fatal error",
        "Crash report saved",
        "Failed to bind to port"
    )

    fun isSuccess(logLine: String): Boolean {
        return SUCCESS_PATTERNS.any { logLine.contains(it, ignoreCase = true) }
    }

    fun isError(logLine: String): Boolean {
        return ERROR_PATTERNS.any { logLine.contains(it, ignoreCase = true) }
    }
}
