import re

with open('app/src/main/java/com/example/server/engine/BaseJavaEngine.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_crash = """                        if (signal == 11) onLog("Signal details: SIGSEGV")
                    }"""

new_crash = """                        if (signal == 11) onLog("Signal details: SIGSEGV")
                    }
                    
                    if (exitCode == 134) {
                        try {
                            onLog("--- Native Crash Logcat Dump ---")
                            val logcatPb = ProcessBuilder("logcat", "-d", "-t", "100")
                            val logcatP = logcatPb.start()
                            val reader = java.io.BufferedReader(java.io.InputStreamReader(logcatP.inputStream))
                            var line: String?
                            while (reader.readLine().also { line = it } != null) {
                                if (line!!.contains("DEBUG") || line!!.contains("crash") || line!!.contains("Fatal")) {
                                    onLog("LOGCAT: $line")
                                }
                            }
                            logcatP.waitFor()
                        } catch (e: Exception) {
                            onLog("Failed to dump logcat: ${e.message}")
                        }
                    }"""

content = content.replace(old_crash, new_crash)

with open('app/src/main/java/com/example/server/engine/BaseJavaEngine.kt', 'w', encoding='utf-8') as f:
    f.write(content)
