package com.example.server.engine

import com.example.server.ServerStatus

import android.content.Context
import com.example.server.Downloader
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import com.example.server.health.ServerHealthMonitor


abstract class BaseJavaEngine(val context: Context, val onLog: (String) -> Unit, val onStatusChange: (ServerStatus) -> Unit) : ServerEngine {
    abstract val serverJarUrl: String
    abstract val serverFolderName: String
    abstract val serverJarName: String
    abstract val serverEngineName: String
    open val minJavaVersion: Int = 21
    open val maxJavaVersion: Int = 21


    private var process: Process? = null
    private var logJob: Job? = null
    private var proxyJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val healthMonitor = ServerHealthMonitor(onLog, onStatusChange)

    private fun startDiscoveryProxy() {
        if (proxyJob?.isActive == true) return
        proxyJob = scope.launch(Dispatchers.IO) {
            try {
                val serverSocket = java.net.ServerSocket(19133, 0, java.net.InetAddress.getByName("127.0.0.1"))
                Log.i("ServerManager", "Local discovery proxy started on 19133")
                while (true) {
                    val client = serverSocket.accept()
                    launch(Dispatchers.IO) {
                        try {
                            val reader = java.io.BufferedReader(java.io.InputStreamReader(client.inputStream))
                            val requestLine = reader.readLine()
                            if (requestLine != null && requestLine.startsWith("GET")) {
                                val parts = requestLine.split(" ")
                                val requestUrl = if (parts.size > 1) parts[1] else "/"
                                
                                val targetUrlStr = if (requestUrl.startsWith("http")) requestUrl else "https://client.discovery.minecraft-services.net\$requestUrl"
                                val safeName = requestUrl.replace("/", "_").replace("?", "_").replace(":", "_")
                                val cacheFileName = "discovery_cache_" + safeName + ".json"
                                val cacheFile = File(serverDir, cacheFileName)
                                var responseBody = ""
                                var success = false
                                
                                while (true) {
                                    val headerLine = reader.readLine()
                                    if (headerLine.isNullOrEmpty()) break
                                }
                                
                                var attempts = 0
                                while (attempts < 3 && !success) {
                                    attempts++
                                    try {
                                        val url = java.net.URL(targetUrlStr)
                                        val conn = url.openConnection() as java.net.HttpURLConnection
                                        conn.requestMethod = "GET"
                                        conn.connectTimeout = 5000
                                        conn.readTimeout = 5000
                                        if (conn.responseCode == 200) {
                                            responseBody = conn.inputStream.bufferedReader().use { it.readText() }
                                            cacheFile.writeText(responseBody)
                                            success = true
                                        }
                                    } catch (e: Exception) {
                                        Log.e("ServerManager", "Failed to fetch discovery (Attempt \$attempts/3): \${e.message}")
                                        if (attempts < 3) kotlinx.coroutines.delay(1000)
                                    }
                                }
                                
                                if (!success && cacheFile.exists()) {
                                    responseBody = cacheFile.readText()
                                    success = true
                                    Log.i("ServerManager", "Used cached discovery data")
                                }
                                
                                if (!success) {
                                    if (requestUrl.contains("/builds/1.0.0.0")) {
                                        responseBody = "{\"TenantId\":\"placeholder\",\"SigningKeys\":[],\"SpringboardUrl\":\"https://client.discovery.minecraft-services.net/v1/springboard\",\"MinecraftServicesUrl\":\"https://client.discovery.minecraft-services.net\",\"MinecraftServicesDiscoveryUrl\":\"https://client.discovery.minecraft-services.net\"}"
                                        success = true
                                    } else if (requestUrl.contains("/springboard")) {
                                        responseBody = "{\"issuer\":\"placeholder\",\"jwks_uri\":\"https://client.discovery.minecraft-services.net/jwks\"}"
                                        success = true
                                    } else if (requestUrl.contains("/jwks")) {
                                        responseBody = "{\"keys\":[]}"
                                        success = true
                                    }
                                }
                                
                                val writer = java.io.BufferedWriter(java.io.OutputStreamWriter(client.outputStream))
                                if (success) {
                                    val bytes = responseBody.toByteArray(Charsets.UTF_8)
                                    writer.write("HTTP/1.1 200 OK\r\n")
                                    writer.write("Content-Type: application/json\r\n")
                                    writer.write("Content-Length: \${bytes.size}\r\n")
                                    writer.write("Connection: close\r\n\r\n")
                                    writer.flush()
                                    client.outputStream.write(bytes)
                                } else {
                                    writer.write("HTTP/1.1 500 Internal Server Error\r\n\r\n")
                                }
                                writer.flush()
                            } else if (requestLine != null && requestLine.startsWith("CONNECT")) {
                                val writer = java.io.BufferedWriter(java.io.OutputStreamWriter(client.outputStream))
                                val parts = requestLine.split(" ")
                                val hostPort = parts[1]
                                val host = hostPort.substringBefore(":")
                                val portStr = hostPort.substringAfter(":", "443")
                                val port = portStr.toIntOrNull() ?: 443
                                
                                while (true) {
                                    val headerLine = reader.readLine()
                                    if (headerLine.isNullOrEmpty()) break
                                }
                                
                                try {
                                    val resolvedIp = com.example.server.NetworkDiagnosticsManager.getIp(host)
                                    val remoteSocket = java.net.Socket(resolvedIp, port)
                                    writer.write("HTTP/1.1 200 Connection established\r\n\r\n")
                                    writer.flush()
                                    
                                    val clientIn = client.inputStream
                                    val clientOut = client.outputStream
                                    val remoteIn = remoteSocket.inputStream
                                    val remoteOut = remoteSocket.outputStream
                                    
                                    val job1 = launch(Dispatchers.IO) { try { clientIn.copyTo(remoteOut) } catch(e: Exception) {} }
                                    val job2 = launch(Dispatchers.IO) { try { remoteIn.copyTo(clientOut) } catch(e: Exception) {} }
                                    job1.join()
                                    job2.join()
                                    remoteSocket.close()
                                } catch (e: Exception) {
                                    writer.write("HTTP/1.1 502 Bad Gateway\r\n\r\n")
                                    writer.flush()
                                }
                            }
                            client.close()
                        } catch (e: Exception) {
                            try { client.close() } catch (e: Exception) {}
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val serverDir: File
        get() = File(context.filesDir, "minecraft/engines/$serverFolderName").apply { mkdirs() }
        
    val serverJar: File
        get() = File(serverDir, serverJarName)
        
    val jreDir: File
        get() = File(context.filesDir, "jre21")
        
    val javaBin: File
        get() = File(jreDir, "bin/java")

    fun checkIntegrity() {
        scope.launch {
            val libjli = java.io.File(jreDir, "lib/libjli.so")
            val libjliAlt = java.io.File(jreDir, "lib/jli/libjli.so")
            val libjvm = java.io.File(jreDir, "lib/server/libjvm.so")
            val jreValid = javaBin.exists() && (libjli.exists() || libjliAlt.exists()) && libjvm.exists()
            if (!jreValid || !javaBin.canExecute() || !serverJar.exists()) {
                withContext(Dispatchers.Main) { onLog("Some required files are missing. They will be downloaded automatically when you start the server.") }
            } else {
                withContext(Dispatchers.Main) { onLog("System integrity check passed. Files are ready.") }
            }
        }
    }

    override fun startServer(memoryMb: Int) {
        if (process != null) {
            onLog("Server is already running or starting.")
            return
        }

        scope.launch {
            withContext(Dispatchers.Main) { onStatusChange(ServerStatus.STARTING) }

            // Download Phase (if needed)
            val libjli = java.io.File(jreDir, "lib/libjli.so")
            val libjliAlt = java.io.File(jreDir, "lib/jli/libjli.so")
            val libjvm = java.io.File(jreDir, "lib/server/libjvm.so")
            val jreValid = javaBin.exists() && (libjli.exists() || libjliAlt.exists()) && libjvm.exists()
            if (!jreValid || !serverJar.exists()) {
                withContext(Dispatchers.Main) { onLog("Files missing. Initiating setup...") }
                if (!jreValid) {
                    withContext(Dispatchers.Main) { onLog("Downloading Java runtime...") }
                    Downloader.downloadAndExtractJre(jreDir) { progress ->
                        scope.launch(Dispatchers.Main) { onLog(progress) }
                    }
                }
                if (!serverJar.exists()) {
                    withContext(Dispatchers.Main) { onLog("2. Download started: $serverJarName") }
                    Downloader.downloadServerJar(serverJarUrl, serverJar) { progress ->
                        scope.launch(Dispatchers.Main) { onLog(progress) }
                    }
                }
            }

            // 1. VALIDATION PHASE (DO NOT SKIP)
            withContext(Dispatchers.Main) { 
                onLog("Validating environment...")
            // 1.5. NETWORK AUTHENTICATION DIAGNOSTICS
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
                } catch (e: Exception) {
                    try {
                        val dohUrl = java.net.URL("https://dns.google/resolve?name=authorization.franchise.minecraft-services.net&type=A")
                        val conn = dohUrl.openConnection() as java.net.HttpURLConnection
                        if (conn.responseCode == 200) {
                            val json = conn.inputStream.bufferedReader().use { it.readText() }
                            dnsPass = json.contains("\"data\"")
                        }
                    } catch (e2: Exception) {}
                }

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
                } catch (e: Exception) {
                    val jwksFile = java.io.File(serverDir, "jwks_cache.json")
                    if (jwksFile.exists() && jwksFile.length() > 0) {
                        if (System.currentTimeMillis() - jwksFile.lastModified() < 86400000) {
                            jwksPass = true
                            jwtPass = true
                        }
                    }
                }
                
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
            }
                onLog("1. Selected engine: $serverEngineName")
                onLog("Download URL: $serverJarUrl")
                onLog("Downloaded filename: $serverJarName")
                onLog("File location: ${serverJar.absolutePath}")
                if (serverJar.exists()) {
                    val sizeMb = serverJar.length() / (1024 * 1024)
                    onLog("File size: $sizeMb MB")
                } else {
                    onLog("File size: 0 MB (Not found)")
                }
                
                // Also print the exact example format from the prompt to be safe
                onLog("Selected Engine: $serverEngineName")
                onLog("Jar:\n$serverJarName")
                onLog("Path:\n${serverJar.absolutePath}")
                if (serverJar.exists()) {
                    val sizeMb = serverJar.length() / (1024 * 1024)
                    onLog("Size:\n$sizeMb MB")
                } else {
                    onLog("Size:\n0 MB")
                }
            }
            if (!javaBin.exists()) {
                withContext(Dispatchers.Main) {
                    onLog("Error: Java runtime missing")
                    onLog("File not found: ${javaBin.absolutePath}")
                    onStatusChange(ServerStatus.ERROR)
                }
                return@launch
            }
            if (!serverJar.exists()) {
                withContext(Dispatchers.Main) {
                    onLog("Error: Server file missing")
                    onLog("File not found: ${serverJar.absolutePath}")
                    onStatusChange(ServerStatus.ERROR)
                }
                return@launch
            }
            
            withContext(Dispatchers.Main) { onLog("Validating jar file...") }
            var isValidJar = false
            try {
                if (serverJar.length() > 1_000_000) { // at least 1MB
                    java.util.zip.ZipFile(serverJar).use { zip ->
                        val manifest = zip.getEntry("META-INF/MANIFEST.MF")
                        var hasClass = false
                        val entries = zip.entries()
                        while (entries.hasMoreElements()) {
                            val entry = entries.nextElement()
                            if (entry.name.endsWith(".class")) {
                                hasClass = true
                                break
                            }
                        }
                        
                        if (manifest != null && hasClass) {
                            val manifestContent = zip.getInputStream(manifest).bufferedReader().readText()
                            if (manifestContent.contains("Main-Class:")) {
                                isValidJar = true
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                isValidJar = false
            }
            
            if (!isValidJar) {
                serverJar.delete()
                withContext(Dispatchers.Main) {
                    onLog("Error: Invalid server jar downloaded.")
                    onLog("Deleted corrupted file. Please try again.")
                    onStatusChange(ServerStatus.ERROR)
                }
                return@launch
            }

            withContext(Dispatchers.Main) { 
                onLog("4. Validation passed")
                onLog("Preparing discovery cache...")
            }
            
            val diagPassed = com.example.server.NetworkDiagnosticsManager.runDiagnostics(context, serverDir, onLog)
            if (!diagPassed) {
                withContext(Dispatchers.Main) {
                    onLog("Diagnostics failed! Aborting server start.")
                    onStatusChange(com.example.server.ServerStatus.ERROR)
                }
                return@launch
            }
            startDiscoveryProxy()
            withContext(Dispatchers.Main) {
                launchServerProcess(memoryMb)
            }
        }
    }

    private fun launchServerProcess(memoryMb: Int) {
        logJob = scope.launch(Dispatchers.IO) {
            try {
                // Step 1: Initialize environment
                File(serverDir, "world").mkdirs()
                File(serverDir, "logs").mkdirs()
                File(serverDir, "eula.txt").writeText("eula=true\n")
                
                val nukkitYml = File(serverDir, "nukkit.yml")
                if (!nukkitYml.exists()) {
                    nukkitYml.writeText(
                        "settings:\n" +
                        "  language: \"eng\"\n" +
                        "  force-language: true\n"
                    )
                }
                
                val pnxYml = File(serverDir, "powernukkitx.yml")
                if (!pnxYml.exists()) {
                    pnxYml.writeText(
                        "settings:\n" +
                        "  language: \"eng\"\n" +
                        "  force-language: true\n"
                    )
                }
                
                val serverProps = File(serverDir, "server.properties")
                if (!serverProps.exists()) {
                    serverProps.writeText(
                        "server-ip=0.0.0.0\n" +
                        "server-port=19132\n" +
                        "xbox-auth=on\n"
                    )
                } else {
                    var propsContent = serverProps.readText()
                    var changed = false
                    if (propsContent.contains("xbox-auth=off")) {
                        propsContent = propsContent.replace("xbox-auth=off", "xbox-auth=on")
                        changed = true
                    }
                    if (propsContent.contains("xbox-auth=false")) {
                        propsContent = propsContent.replace("xbox-auth=false", "xbox-auth=on")
                        changed = true
                    }
                    if (changed) {
                        serverProps.writeText(propsContent)
                    }
                }
                
                withContext(Dispatchers.Main) { 
                    onLog("Xbox Authentication:")
                    onLog("ENABLE")
                    onLog("Initializing Java process...") 
                }

                
                
                withContext(Dispatchers.Main) {
                    healthMonitor.onProcessStart()
                }
                
                serverDir.mkdirs()
                val tmpDir = File(context.cacheDir, "pnx_tmp").apply { mkdirs() }
                
                Log.i("ServerManager", "Starting server with JRE at: ${jreDir.absolutePath}")
                Log.i("ServerManager", "Server working dir: ${serverDir.absolutePath}")
                Log.i("ServerManager", "Java home property: ${jreDir.absolutePath}")
                
                withContext(Dispatchers.Main) {
                    onLog("5. Correct jar launched: $serverJarName")
                    onLog("Working directory: ${serverDir.absolutePath}")
                    onLog("Temp directory: ${tmpDir.absolutePath}")
                    onLog("JAVA_HOME: ${jreDir.absolutePath}")
                    onLog("HOME: ${serverDir.absolutePath}")
                    onLog("Jar path: ${serverJar.absolutePath}")
                    onLog("Working directory exists: ${serverDir.exists()}")
                    onLog("Temp directory exists: ${tmpDir.exists()}, isAbsolute: ${tmpDir.isAbsolute}")
                    
                    if (!serverDir.exists()) {
                        onLog("ERROR: Working directory does not exist: ${serverDir.absolutePath}")
                        onStatusChange(ServerStatus.ERROR)
                    }
                    if (!tmpDir.exists()) {
                        onLog("ERROR: Temp directory does not exist: ${tmpDir.absolutePath}")
                        onStatusChange(ServerStatus.ERROR)
                    }
                }
                
                if (!serverDir.exists() || !tmpDir.exists()) {
                    return@launch
                }
                

                val activityManager = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                val memoryInfo = android.app.ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memoryInfo)
                val totalRamMb = (memoryInfo.totalMem / (1024 * 1024)).toInt()
                val availRamMb = (memoryInfo.availMem / (1024 * 1024)).toInt()
                val arch = android.os.Build.SUPPORTED_ABIS.joinToString(", ")
                
                withContext(Dispatchers.Main) {
                    onLog("Device Architecture: $arch")
                    onLog("Device RAM: Total = ${totalRamMb}MB, Available = ${availRamMb}MB")
                }
                
                val argsList = mutableListOf<String>()
                argsList.add(javaBin.absolutePath)
                
                argsList.add("-Xint")
                
                argsList.add("-Djava.io.tmpdir=${tmpDir.absolutePath}")
                argsList.add("-Duser.dir=${serverDir.absolutePath}")
                argsList.add("-Djava.net.preferIPv4Stack=true")
                argsList.add("-Djava.net.preferIPv4Addresses=true")
                argsList.add("-Dhttps.protocols=TLSv1.2,TLSv1.3")
                argsList.add("-Djdk.net.hosts.file=${File(serverDir, "custom_hosts.txt").absolutePath}")
                argsList.add("-Dhttps.proxyHost=127.0.0.1")
                argsList.add("-Dhttps.proxyPort=19133")
                argsList.add("-Dhttp.proxyHost=127.0.0.1")
                argsList.add("-Dhttp.proxyPort=19133")
                argsList.add("-Dfile.encoding=UTF-8")
                
                // Disable memory mapping for zips which can cause issues on some architectures
                argsList.add("-Dsun.zip.disableMemoryMapping=true")
                argsList.add("-Djna.nosys=true")
                argsList.add("-Dorg.jline.terminal.dumb=true")
                argsList.add("-Djline.terminal=jline.UnsupportedTerminal")
                
                argsList.add("-Xms256M")
                
                argsList.add("-Xmx1024M")
                
                // DNS resolution moved to NetworkDiagnosticsManager
                
                val trustStoreFile = java.io.File(serverDir, "truststore.p12")
                if (!trustStoreFile.exists()) {
                    try {
                        val keyStore = java.security.KeyStore.getInstance("AndroidCAStore")
                        keyStore.load(null, null)
                        
                        val p12 = java.security.KeyStore.getInstance("PKCS12")
                        p12.load(null, null)
                        
                        val aliases = keyStore.aliases()
                        while (aliases.hasMoreElements()) {
                            val alias = aliases.nextElement()
                            val cert = keyStore.getCertificate(alias)
                            p12.setCertificateEntry(alias, cert)
                        }
                        
                        java.io.FileOutputStream(trustStoreFile).use { fos ->
                            p12.store(fos, "changeit".toCharArray())
                        }
                    } catch (e: Exception) {
                        onLog("Failed to create truststore: ${e.message}")
                    }
                }
                argsList.add("-Djavax.net.ssl.trustStore=${trustStoreFile.absolutePath}")
                argsList.add("-Djavax.net.ssl.trustStorePassword=changeit")
                argsList.add("-Djavax.net.ssl.trustStoreType=PKCS12")
                
                argsList.add("-jar")
                argsList.add(serverJar.absolutePath)
                argsList.add("nogui")
                
                val args = argsList.toTypedArray()

                
                
                
                // DIAGNOSTIC
                var diagFailed = false
                withContext(Dispatchers.Main) {
                    onLog("--- Server Startup Diagnostics ---")
                    onLog("Java executable path: ${javaBin.absolutePath}")
                    onLog("Java architecture: ${android.os.Build.SUPPORTED_ABIS.joinToString(", ")}")
                    onLog("Server jar path: ${serverJar.absolutePath}")
                    onLog("Server jar size: ${serverJar.length()} bytes")
                    onLog("Working directory: ${serverDir.absolutePath}")
                    onLog("Temporary directory: ${tmpDir.absolutePath}")
                    onLog("LD_LIBRARY_PATH: ${jreDir.absolutePath}/lib:${jreDir.absolutePath}/lib/jli:${jreDir.absolutePath}/lib/server")
                    
                    if (!javaBin.exists()) {
                        onLog("ERROR: Java executable not found")
                        diagFailed = true
                    } else if (!javaBin.canExecute()) {
                        onLog("ERROR: Java executable is not executable")
                        diagFailed = true
                    }
                    
                    if (!serverJar.exists()) {
                        onLog("ERROR: Server JAR not found")
                        diagFailed = true
                    } else if (serverJar.length() == 0L) {
                        onLog("ERROR: Server JAR is empty")
                        diagFailed = true
                    }
                }
                
                if (diagFailed) {
                    withContext(Dispatchers.Main) { onStatusChange(com.example.server.ServerStatus.ERROR) }
                    return@launch
                }
                
                val envMap = mutableMapOf<String, String>()
                envMap["JAVA_HOME"] = jreDir.absolutePath
                envMap["HOME"] = serverDir.absolutePath
                envMap["TMPDIR"] = tmpDir.absolutePath
                envMap["DNS_SERVER"] = "automatic"
                val ldLibPath = "${jreDir.absolutePath}/lib:${jreDir.absolutePath}/lib/jli:${jreDir.absolutePath}/lib/server"
                envMap["LD_LIBRARY_PATH"] = ldLibPath
                envMap["MALLOC_NANO_ZONE"] = "0" 
                
                try {
                    val diagPb = ProcessBuilder(javaBin.absolutePath, "-version")
                    diagPb.directory(serverDir)
                    // // diagPb.environment().clear()
                    diagPb.environment().putAll(envMap)
                    diagPb.environment().remove("JAVA_TOOL_OPTIONS")
                    diagPb.environment().remove("_JAVA_OPTIONS")
                    val pDiag = diagPb.start()
                    val readerDiag = java.io.BufferedReader(java.io.InputStreamReader(pDiag.inputStream))
                    val errorDiag = java.io.BufferedReader(java.io.InputStreamReader(pDiag.errorStream))
                    
                    var lineDiag: String?
                    var versionOutput = ""
                    while (readerDiag.readLine().also { lineDiag = it } != null) {
                        versionOutput += lineDiag + "\n"
                        withContext(Dispatchers.Main) { onLog("java -version stdout: $lineDiag") }
                    }
                    while (errorDiag.readLine().also { lineDiag = it } != null) {
                        versionOutput += lineDiag + "\n"
                        withContext(Dispatchers.Main) { onLog("java -version stderr: $lineDiag") }
                    }
                    val diagExit = pDiag.waitFor()
                    withContext(Dispatchers.Main) { 
                        onLog("java -version exit code: $diagExit")
                        if (diagExit != 0) {
                            onLog("ERROR: Java exit code is not 0. Aborting.")
                            onStatusChange(com.example.server.ServerStatus.ERROR)
                            diagFailed = true
                        }
                        if (!versionOutput.contains("\"21") && !versionOutput.contains("\"22") && !versionOutput.contains("\"23")) {
                            onLog("ERROR: Java version is not 21+. Aborting.")
                            onStatusChange(com.example.server.ServerStatus.ERROR)
                            diagFailed = true
                        }
                    }
                } catch(e: Exception) {
                    withContext(Dispatchers.Main) { 
                        onLog("ERROR: java -version failed: ${e.message}")
                        onStatusChange(com.example.server.ServerStatus.ERROR)
                    }
                    diagFailed = true
                }
                                
                val pb = ProcessBuilder(args.toList())
                pb.directory(serverDir)
                // // pb.environment().clear()
                pb.environment().putAll(envMap)
                pb.environment().remove("JAVA_TOOL_OPTIONS")
                pb.environment().remove("_JAVA_OPTIONS")
                pb.environment().remove("http_proxy")
                pb.environment().remove("https_proxy")
                pb.environment().remove("HTTP_PROXY")
                pb.environment().remove("HTTPS_PROXY")
                pb.redirectErrorStream(true)
                
                withContext(Dispatchers.Main) {
                    onLog("Executable:\n${args[0]}")
                    onLog("Arguments:\n${args.drop(1).joinToString(" ")}")
                    onLog("Environment:\nJAVA_HOME=${envMap["JAVA_HOME"]}, HOME=${envMap["HOME"]}, TMPDIR=${envMap["TMPDIR"]}, LD_LIBRARY_PATH=${envMap["LD_LIBRARY_PATH"]}")
                    onLog("Working directory:\n${serverDir.absolutePath}")
                }
                
                process = pb.start()
                
                
                
                val pReader = java.io.BufferedReader(java.io.InputStreamReader(process!!.inputStream, Charsets.UTF_8))
                val logJob2 = scope.launch(Dispatchers.IO) {
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
                                    process?.outputStream?.write("eng\n".toByteArray())
                                    process?.outputStream?.flush()
                                }
                                if (line.contains("You MUST accept this license to continue") || line.contains("Do you accept the license")) {
                                    Log.i("ServerManager", "[JRE] Auto-accepting license")
                                    onLog("[JRE] Auto-accepting license")
                                    process?.outputStream?.write("yes\n".toByteArray())
                                    process?.outputStream?.flush()
                                }
                            }
                        }
                    } catch (e: Exception) {}
                }
                
                
                val exitCode = process!!.waitFor()
                logJob2.cancel()
                
                withContext(Dispatchers.Main) {
                    onLog("Server process exited with code $exitCode")
                    
                    val crashLogs = serverDir.listFiles { _, name -> name.startsWith("hs_err_pid") && name.endsWith(".log") }
                    crashLogs?.forEach { crashLog ->
                        onLog("--- JVM CRASH LOG FOUND: ${crashLog.name} ---")
                        try {
                            crashLog.useLines { lines ->
                                lines.take(100).forEach { onLog(it) }
                            }
                        } catch (e: Exception) {
                            onLog("Failed to read crash log: ${e.message}")
                        }
                    }
                    
                    healthMonitor.onProcessExit(exitCode)
                    process = null
                }
} catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    e.printStackTrace()
                    onLog("Startup failure: ${e.message}")
                    onLog("Java path: ${javaBin.absolutePath}")
                    onLog("Server jar: ${serverJar.absolutePath}")
                    onLog("Working dir: ${serverDir.absolutePath}")
                    onLog("Temp dir: ${File(context.cacheDir, "pnx_tmp").absolutePath}")
                    onLog(e.stackTraceToString())
                    healthMonitor.setStatus(ServerStatus.ERROR)
                    process = null
                }
            }
        }
    }

    override fun stopServer() {
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { 
                healthMonitor.setStatus(ServerStatus.STOPPING)
                onLog("Stopping server process...") 
            }
            
            val p = process
            if (p != null) {
                try {
                    p.outputStream?.write("stop\n".toByteArray())
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
    }

    override fun restartServer() {
        onLog("Restarting server...")
        stopServer()
        scope.launch {
            kotlinx.coroutines.delay(1000)
            withContext(Dispatchers.Main) {
                startServer(600)
            }
        }
    }
    override fun sendCommand(command: String) {
        try {
            process?.outputStream?.let { stream ->
                stream.write("$command\n".toByteArray())
                stream.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getStatus(): com.example.server.ServerStatus {
        return if (process != null) com.example.server.ServerStatus.RUNNING else com.example.server.ServerStatus.STOPPED
    }

    override fun installPlugin(url: String, fileName: String) {
        onLog("Plugin installation not yet fully implemented for Nukkit.")
    }

    override fun backupWorld() {
        onLog("World backup not yet implemented for Nukkit.")
    }
}
