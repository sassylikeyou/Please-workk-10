import re

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "r") as f:
    content = f.read()

replacement = """    override fun stopServer() {
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { 
                healthMonitor.setStatus(ServerStatus.STOPPING)
                onLog("Stopping server process...") 
            }
            
            val p = process
            if (p != null) {
                try {
                    p.outputStream?.write("stop\\n".toByteArray())
                    p.outputStream?.flush()
                } catch (e: Exception) {
                    // Ignore exception
                }

                var waited = 0
                while (waited < 100) { // Wait up to 10 seconds for graceful shutdown
                    try {
                        p.exitValue()
                        break // Process ended
                    } catch (e: IllegalThreadStateException) {
                        // Still running
                    }
                    kotlinx.coroutines.delay(100)
                    waited++
                }

                try { p.inputStream?.close() } catch (e: Exception) {}
                try { p.outputStream?.close() } catch (e: Exception) {}
                try { p.errorStream?.close() } catch (e: Exception) {}

                try {
                    p.exitValue()
                } catch (e: IllegalThreadStateException) {
                    // Process still alive
                    p.destroy()
                    kotlinx.coroutines.delay(500)
                    try {
                        p.exitValue()
                    } catch (e2: IllegalThreadStateException) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            p.destroyForcibly()
                        }
                    }
                }
            }

            process = null
            logJob?.cancel()
            logJob = null
            
            withContext(Dispatchers.Main) { 
                healthMonitor.setStatus(ServerStatus.STOPPED)
                onLog("Server stopped.")
            }
        }
    }"""

pattern = r"    override fun stopServer\(\) \{.*?\n    \}"
content = re.sub(pattern, replacement, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "w") as f:
    f.write(content)
