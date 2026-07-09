import re

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "r") as f:
    content = f.read()

replacement = """                val logJob2 = scope.launch(Dispatchers.IO) {
                    try {
                        pReader.forEachLine { line ->
                            scope.launch(Dispatchers.Main) { 
                                onLog(line)
                                healthMonitor.analyzeLogLine(line)
                                if (line.contains("Listening on ")) {
                                    Log.i("ServerManager", "[DEBUG] Current server bind address and Port: $line")
                                }
                                if (line.contains("UDP socket") || line.contains("RakNet")) {
                                    Log.i("ServerManager", "[DEBUG] UDP socket opened successfully: $line")
                                }
                                if (line.contains("Enter a language code from the list below")) {
                                    Log.i("ServerManager", "[JRE] First run detected")
                                    onLog("[JRE] Applying default language: eng")
                                    process?.outputStream?.write("eng\\n".toByteArray())
                                    process?.outputStream?.flush()
                                }
                                if (line.contains("You MUST accept this license to continue") || line.contains("Do you accept the license")) {
                                    Log.i("ServerManager", "[JRE] Auto-accepting license")
                                    onLog("[JRE] Auto-accepting license")
                                    process?.outputStream?.write("yes\\n".toByteArray())
                                    process?.outputStream?.flush()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore stream closed exceptions
                    }
                }"""

pattern = r"                val logJob2 = scope\.launch\(Dispatchers\.IO\) \{.*?                \}"
content = re.sub(pattern, replacement, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "w") as f:
    f.write(content)
