import re

with open('app/src/main/java/com/example/server/engine/BaseJavaEngine.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Remove pb.redirectErrorStream(true)
content = content.replace("pb.redirectErrorStream(true)", "pb.redirectErrorStream(false)")

old_reader = """                val pReader = java.io.BufferedReader(java.io.InputStreamReader(process!!.inputStream, Charsets.UTF_8))
                val logJob2 = scope.launch(Dispatchers.IO) {
                    try {
                        pReader.forEachLine { line ->"""

new_reader = """                val stdoutBuffer = java.util.concurrent.ConcurrentLinkedQueue<String>()
                val stderrBuffer = java.util.concurrent.ConcurrentLinkedQueue<String>()

                val pReader = java.io.BufferedReader(java.io.InputStreamReader(process!!.inputStream, Charsets.UTF_8))
                val pErrorReader = java.io.BufferedReader(java.io.InputStreamReader(process!!.errorStream, Charsets.UTF_8))
                
                val logJob2 = scope.launch(Dispatchers.IO) {
                    try {
                        pReader.forEachLine { line ->
                            stdoutBuffer.offer(line)
                            if (stdoutBuffer.size > 200) stdoutBuffer.poll()"""

content = content.replace(old_reader, new_reader)

old_wait = """                }
                
                
                val exitCode = process!!.waitFor()
                logJob2.cancel()
                
                withContext(Dispatchers.Main) {
                    onLog("Server process exited with code $exitCode")"""

new_wait = """                }

                val logJob3 = scope.launch(Dispatchers.IO) {
                    try {
                        pErrorReader.forEachLine { line ->
                            stderrBuffer.offer(line)
                            if (stderrBuffer.size > 200) stderrBuffer.poll()
                            scope.launch(Dispatchers.Main) { 
                                onLog("[STDERR] $line")
                            }
                        }
                    } catch (e: Exception) {}
                }
                
                val exitCode = process!!.waitFor()
                logJob2.cancel()
                logJob3.cancel()
                
                withContext(Dispatchers.Main) {
                    onLog("Server process exited with code $exitCode")
                    
                    if (exitCode != 0) {
                        onLog("--- CRASH DIAGNOSTICS ---")
                        val signal = if (exitCode > 128) exitCode - 128 else null
                        onLog("Exit code: $exitCode")
                        if (signal != null) {
                            onLog("Signal causing termination: $signal")
                            if (signal == 6) onLog("Signal details: SIGABRT")
                            if (signal == 9) onLog("Signal details: SIGKILL")
                            if (signal == 11) onLog("Signal details: SIGSEGV")
                        }
                        onLog("--- STDOUT (last 50 lines) ---")
                        stdoutBuffer.toList().takeLast(50).forEach { onLog(it) }
                        onLog("--- STDERR (last 50 lines) ---")
                        stderrBuffer.toList().takeLast(50).forEach { onLog(it) }
                        onLog("-------------------------")
                    }"""

content = content.replace(old_wait, new_wait)

if "CRASH DIAGNOSTICS" not in content:
    print("Failed to replace!")

with open('app/src/main/java/com/example/server/engine/BaseJavaEngine.kt', 'w', encoding='utf-8') as f:
    f.write(content)
