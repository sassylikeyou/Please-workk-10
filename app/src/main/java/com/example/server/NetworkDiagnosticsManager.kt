package com.example.server

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Dns
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException

object NetworkDiagnosticsManager {

    private const val TAG = "NetworkDiag"
    
    val resolvedIps = ConcurrentHashMap<String, String>()

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> {
                    val ipStr = resolvedIps[hostname]
                    if (ipStr != null) {
                        return listOf(InetAddress.getByName(ipStr))
                    }
                    return Dns.SYSTEM.lookup(hostname)
                }
            })
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    suspend fun runDiagnostics(context: Context, serverDir: File, onLog: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        withContext(Dispatchers.Main) { onLog("NETWORK TEST:") }
        
        // 1. Internet Permission
        val hasInternet = context.checkSelfPermission(android.Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED
        if (hasInternet) {
            withContext(Dispatchers.Main) { onLog("Internet: PASS") }
        } else {
            withContext(Dispatchers.Main) { onLog("Internet: FAIL") }
            return@withContext false
        }

        // 2. Resolve DNS
        val domainDiscovery = "client.discovery.minecraft-services.net"
        val domainAuth = "authorization.franchise.minecraft-services.net"
        
        val ipDiscovery = resolveDomain(domainDiscovery)
        if (ipDiscovery != null) {
            resolvedIps[domainDiscovery] = ipDiscovery
            withContext(Dispatchers.Main) { onLog("DNS $domainDiscovery: PASS") }
        } else {
            withContext(Dispatchers.Main) { onLog("DNS $domainDiscovery: FAIL") }
            return@withContext false
        }

        val ipAuth = resolveDomain(domainAuth)
        if (ipAuth != null) {
            resolvedIps[domainAuth] = ipAuth
            withContext(Dispatchers.Main) { onLog("DNS $domainAuth: PASS") }
        } else {
            withContext(Dispatchers.Main) { onLog("DNS $domainAuth: FAIL") }
            return@withContext false
        }



        // 3. HTTPS Minecraft discovery
        val discoveryUrl = "https://client.discovery.minecraft-services.net/api/v1.0/discovery/MinecraftPE/builds/1.0.0.0"
        val discoveryPass = checkHttps(discoveryUrl, ipDiscovery, domainDiscovery, onLog)
        if (discoveryPass) {
            withContext(Dispatchers.Main) { onLog("HTTPS Minecraft discovery: PASS") }
        } else {
            withContext(Dispatchers.Main) { onLog("HTTPS Minecraft discovery: FAIL") }
            return@withContext false
        }

        // 4. Xbox JWKS
        val authUrl = "https://authorization.franchise.minecraft-services.net/.well-known/keys"
        val authPass = checkHttps(authUrl, ipAuth, domainAuth, onLog)
        if (authPass) {
            withContext(Dispatchers.Main) { onLog("Xbox JWKS: PASS") }
        } else {
            withContext(Dispatchers.Main) { onLog("Xbox JWKS: FAIL") }
            return@withContext false
        }

        return@withContext true
    }

    fun getIp(domain: String): String {
        return resolvedIps[domain] ?: domain
    }

    private fun resolveDomain(domain: String): String? {
        try {
            val addr = InetAddress.getByName(domain)
            return addr.hostAddress
        } catch (e: Exception) {
            Log.e(TAG, "Standard DNS failed for $domain, trying DoH")
        }
        
        // Try Google DoH
        val googleIp = tryDoH("https://dns.google/resolve?name=$domain&type=A")
        if (googleIp != null) return googleIp
        
        // Try Cloudflare DoH
        val cloudflareIp = tryDoH("https://cloudflare-dns.com/dns-query?name=$domain&type=A")
        if (cloudflareIp != null) return cloudflareIp

        return null
    }

    private fun tryDoH(urlStr: String): String? {
        try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("accept", "application/dns-json")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonStr)
                if (json.has("Answer")) {
                    val answers = json.getJSONArray("Answer")
                    for (i in 0 until answers.length()) {
                        val answer = answers.getJSONObject(i)
                        if (answer.getInt("type") == 1) { // A record
                            return answer.getString("data")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "DoH failed: ${e.message}")
        }
        return null
    }

    private suspend fun checkHttps(urlStr: String, ip: String, host: String, onLog: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(urlStr)
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            val handshake = response.handshake
            withContext(Dispatchers.Main) {
                onLog("TLS Handshake successful for $host")
                if (handshake != null) {
                    onLog("- TLS Protocol: ${handshake.tlsVersion.javaName}")
                    onLog("- Cipher Suite: ${handshake.cipherSuite.javaName}")
                    val certs = handshake.peerCertificates
                    if (certs.isNotEmpty()) {
                        val cert = certs[0] as java.security.cert.X509Certificate
                        onLog("- Certificate Subject: ${cert.subjectDN.name}")
                        onLog("- Certificate Issuer: ${cert.issuerDN.name}")
                    }
                }
                onLog("- Hostname verification: PASS")
                onLog("- HTTP Response Code: ${response.code}")
            }
            
            return@withContext response.isSuccessful
        } catch (e: SSLHandshakeException) {
            withContext(Dispatchers.Main) { onLog("SSL Handshake exception for $urlStr: ${e.message}") }
            return@withContext false
        } catch (e: SSLPeerUnverifiedException) {
            withContext(Dispatchers.Main) { onLog("Hostname verification failed for $urlStr: ${e.message}") }
            return@withContext false
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onLog("HTTPS check failed for $urlStr: ${e.message}") }
            return@withContext false
        }
    }


}
