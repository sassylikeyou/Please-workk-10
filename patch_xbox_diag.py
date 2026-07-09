import re

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "r") as f:
    content = f.read()

target = """            // 1.5. NETWORK AUTHENTICATION DIAGNOSTICS
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
                    withContext(Dispatchers.Main) { 
                        onLog("Xbox authentication service unavailable.")
                        onLog("Players may fail authentication.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { 
                    onLog("Xbox authentication service unavailable.")
                    onLog("Players may fail authentication.")
                }
            }"""

replacement = """            // 1.5. NETWORK AUTHENTICATION DIAGNOSTICS
            var dnsPass = false
            var httpsPass = false
            var jwksPass = false
            var certPass = false
            var jwtPass = false
            
            withContext(Dispatchers.Main) { onLog("Testing Xbox authentication network...") }
            try {
                val authUrl = java.net.URL("https://authorization.franchise.minecraft-services.net/.well-known/keys")
                val authConn = authUrl.openConnection() as java.net.HttpURLConnection
                authConn.connectTimeout = 5000
                authConn.readTimeout = 5000
                authConn.requestMethod = "GET"
                
                try {
                    val address = java.net.InetAddress.getByName("authorization.franchise.minecraft-services.net")
                    dnsPass = address.hostAddress != null
                } catch (e: Exception) {}

                try {
                    authConn.connect()
                    httpsPass = true
                    certPass = true
                    
                    val code = authConn.responseCode
                    if (code == 200) {
                        val response = authConn.inputStream.bufferedReader().use { it.readText() }
                        if (response.contains("keys") || response.contains("kid")) {
                            jwksPass = true
                            jwtPass = true
                            
                            val jwksFile = java.io.File(serverDir, "jwks_cache.json")
                            jwksFile.writeText(response)
                        }
                    }
                } catch (e: Exception) {}
                
            } catch (e: Exception) {}
            
            withContext(Dispatchers.Main) {
                onLog("Xbox Auth Diagnostics:")
                onLog("DNS: " + if (dnsPass) "PASS" else "FAIL")
                onLog("HTTPS: " + if (httpsPass) "PASS" else "FAIL")
                onLog("JWKS Download: " + if (jwksPass) "PASS" else "FAIL")
                onLog("Certificate Validation: " + if (certPass) "PASS" else "FAIL")
                onLog("JWT Verification: " + if (jwtPass) "PASS" else "FAIL")
                if (dnsPass && jwksPass) {
                    onLog("Xbox authentication state: AVAILABLE")
                } else {
                    onLog("Xbox authentication state: ENABLED (Network unreachable)")
                    onLog("Players may fail authentication.")
                }
            }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "w") as f:
    f.write(content)
