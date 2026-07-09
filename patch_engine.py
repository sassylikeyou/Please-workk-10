import re

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "r") as f:
    content = f.read()

# Add IPv4Addresses
content = content.replace('argsList.add("-Djava.net.preferIPv4Stack=true")',
                          'argsList.add("-Djava.net.preferIPv4Stack=true")\n                argsList.add("-Djava.net.preferIPv4Addresses=true")')

# Add DNS_SERVER to envMap
content = content.replace('envMap["TMPDIR"] = tmpDir.absolutePath',
                          'envMap["TMPDIR"] = tmpDir.absolutePath\n                envMap["DNS_SERVER"] = "automatic"')


# Add Network Diagnostic before validating environment
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

content = content.replace('withContext(Dispatchers.Main) { onLog("Validating environment...") }',
                          'withContext(Dispatchers.Main) { onLog("Validating environment...") }\n' + network_test)

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "w") as f:
    f.write(content)
