with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "r") as f:
    text = f.read()

# 1. Stop Server
old_stop = """    override fun stopServer() {
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { 
                healthMonitor.setStatus(ServerStatus.STOPPING)
                onLog("Stopping server process...") 
            }
            try {
                process?.outputStream?.write("stop\\n".toByteArray())
                process?.outputStream?.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            process?.destroy()
            process = null
            logJob?.cancel()
            logJob = null
            withContext(Dispatchers.Main) { 
                healthMonitor.setStatus(ServerStatus.STOPPED)
                onLog("Server stopped.")
            }
        }
    }"""
new_stop = """    override fun stopServer() {
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
text = text.replace(old_stop, new_stop)

# 2. Log Job
old_logjob = """                val logJob2 = scope.launch(Dispatchers.IO) {
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
                }"""
new_logjob = """                val logJob2 = scope.launch(Dispatchers.IO) {
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
                    } catch (e: Exception) {}
                }"""
text = text.replace(old_logjob, new_logjob)


# 3. IPv4 Addr
text = text.replace('argsList.add("-Djava.net.preferIPv4Stack=true")',
                          'argsList.add("-Djava.net.preferIPv4Stack=true")\n                argsList.add("-Djava.net.preferIPv4Addresses=true")')

# 4. DNS Server
text = text.replace('envMap["TMPDIR"] = tmpDir.absolutePath',
                          'envMap["TMPDIR"] = tmpDir.absolutePath\n                envMap["DNS_SERVER"] = "automatic"')


# 5. Network Diagnostics
network_test = """
            // 1.5. NETWORK AUTHENTICATION DIAGNOSTICS
            withContext(Dispatchers.Main) { onLog("Testing Xbox authentication network...") }
            try {
                val authUrl = java.net.URL("https://authorization.franchise.minecraft-services.net/.well-known/keys")
                val authConn = authUrl.openConnection() as java.net.HttpURLConnection
                authConn.connectTimeout = 5000
                authConn.readTimeout = 5000
                authConn.requestMethod = "GET"
                val code = authConn.responseCode
                if (code == 200) {
                    withContext(Dispatchers.Main) { onLog("Internet authentication available.") }
                } else {
                    withContext(Dispatchers.Main) { onLog("Internet authentication unavailable (Code: $code)") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onLog("Internet authentication unavailable") }
            }
"""
text = text.replace('withContext(Dispatchers.Main) { onLog("Validating environment...") }',
                          'withContext(Dispatchers.Main) { onLog("Validating environment...") }\n' + network_test)


# 6. Proxy Removal
text = text.replace("startDiscoveryProxy()", "// startDiscoveryProxy() removed")
text = text.replace('responseBody = responseBody.replace("https://client.discovery.minecraft-services.net", "http://127.0.0.1:19133")',
                          '// proxy replacement removed')

old_offline = 'val offlineData = "{\\"TenantId\\":\\"placeholder\\",\\"SigningKeys\\":[],\\"SpringboardUrl\\":\\"http://127.0.0.1:19133/v1/springboard\\",\\"MinecraftServicesUrl\\":\\"http://127.0.0.1:19133\\",\\"MinecraftServicesDiscoveryUrl\\":\\"http://127.0.0.1:19133\\"}"'
new_offline = 'val offlineData = "{\\"TenantId\\":\\"placeholder\\",\\"SigningKeys\\":[],\\"SpringboardUrl\\":\\"https://client.discovery.minecraft-services.net/v1/springboard\\",\\"MinecraftServicesUrl\\":\\"https://client.discovery.minecraft-services.net\\",\\"MinecraftServicesDiscoveryUrl\\":\\"https://client.discovery.minecraft-services.net\\"}"'
text = text.replace(old_offline, new_offline)

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "w") as f:
    f.write(text)
