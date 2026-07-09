import re

with open('app/src/main/java/com/example/server/engine/BaseJavaEngine.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Remove startDiscoveryProxy function
content = re.sub(r'    private fun startDiscoveryProxy\(\) \{.*?\n    \}\n', '', content, flags=re.DOTALL)

# 2. Start Foreground Service in startServer
start_svc = """        scope.launch {
            withContext(Dispatchers.Main) { onStatusChange(ServerStatus.STARTING) }
            // Start Foreground Service
            val serviceIntent = android.content.Intent(context, com.example.server.ServerForegroundService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }"""
content = content.replace("        scope.launch {\n            withContext(Dispatchers.Main) { onStatusChange(ServerStatus.STARTING) }", start_svc)

# 3. Stop Foreground Service in stopServer
stop_svc = """    override fun stopServer() {
        val serviceIntent = android.content.Intent(context, com.example.server.ServerForegroundService::class.java)
        serviceIntent.action = "STOP"
        context.startService(serviceIntent)"""
content = content.replace("    override fun stopServer() {", stop_svc)

# 4. Modify Launch Arguments
content = content.replace('argsList.add("-Xint")', '')
content = content.replace('argsList.add("-Xms256M")', 'argsList.add("-Xms512M")')
content = content.replace('argsList.add("-Xmx1024M")', 'argsList.add("-Xmx1536M")')

# 5. Fix InputStreamReader logic (Remove Main dispatcher inside tight loop)
old_reader = """                val logJob2 = scope.launch(Dispatchers.IO) {
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
                                if (line.contains("UnknownHostException: authorization.franchise.minecraft-services.net") || line.contains("InvalidJwtException")) {
                                    onLog("Xbox authentication service unavailable.")
                                    onLog("Player authentication failed.")
                                }
                                if (line.contains("logged in with entity id") || line.contains("Player connected") || line.contains("Player authenticated:")) {
                                    onLog("Xbox authentication state: VERIFIED")
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
                    } catch (e: Exception) {}
                }"""

new_reader = """                val logJob2 = scope.launch(Dispatchers.IO) {
                    try {
                        pReader.forEachLine { line ->
                            withContext(Dispatchers.Main) { 
                                onLog(line)
                                healthMonitor.analyzeLogLine(line)
                            }
                            if (line.contains("Listening on ")) {
                                Log.i("ServerManager", "[DEBUG] Current server bind address and Port: $line")
                            }
                            if (line.contains("UDP socket") || line.contains("RakNet")) {
                                Log.i("ServerManager", "[DEBUG] UDP socket opened successfully: $line")
                            }
                            if (line.contains("UnknownHostException: authorization.franchise.minecraft-services.net") || line.contains("InvalidJwtException")) {
                                withContext(Dispatchers.Main) {
                                    onLog("Xbox authentication service unavailable.")
                                    onLog("Player authentication failed.")
                                }
                            }
                            if (line.contains("logged in with entity id") || line.contains("Player connected") || line.contains("Player authenticated:")) {
                                withContext(Dispatchers.Main) { onLog("Xbox authentication state: VERIFIED") }
                            }
                            if (line.contains("Enter a language code from the list below")) {
                                Log.i("ServerManager", "[JRE] First run detected")
                                withContext(Dispatchers.Main) { onLog("[JRE] Applying default language: eng") }
                                process?.outputStream?.write("eng\\n".toByteArray())
                                process?.outputStream?.flush()
                            }
                            if (line.contains("You MUST accept this license to continue") || line.contains("Do you accept the license")) {
                                Log.i("ServerManager", "[JRE] Auto-accepting license")
                                withContext(Dispatchers.Main) { onLog("[JRE] Auto-accepting license") }
                                process?.outputStream?.write("yes\\n".toByteArray())
                                process?.outputStream?.flush()
                            }
                        }
                    } catch (e: Exception) {}
                }"""
content = content.replace(old_reader, new_reader)

with open('app/src/main/java/com/example/server/engine/BaseJavaEngine.kt', 'w', encoding='utf-8') as f:
    f.write(content)
