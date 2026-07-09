package com.example.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.util.zip.ZipInputStream
import java.io.IOException
import java.io.BufferedInputStream

object Downloader {
    private val client = OkHttpClient()

    private const val NUKKIT_URL = "https://github.com/PowerNukkitX/PowerNukkitX/releases/download/2.0.0/powernukkitx.jar"
    private const val JRE_URL = "https://github.com/MojoLauncher/android-openjdk-build-17-25/releases/download/rolling/jre21-pojav.zip"


    suspend fun downloadServerJar(url: String, destination: File, onProgress: (String) -> Unit): Boolean {
        val success = downloadFile(url, destination, onProgress, destination.name, isJar = true)
        return success
    }

    suspend fun downloadAndExtractJre(jreDir: File, onProgress: (String) -> Unit): Boolean {
        var attempts = 0
        while (attempts < 2) {
            attempts++
            if (attempts > 1) {
                onProgress("Retrying JRE setup (Attempt $attempts/2)...")
                jreDir.deleteRecursively()
            }
            
            val zipFile = File(jreDir.parentFile, "jre.zip")
            onProgress("JRE download URL: $JRE_URL")
            val success = downloadFile(JRE_URL, zipFile, onProgress, "JRE")
            
            if (!success) {
                zipFile.delete()
                continue
            }
            
            val extracted = withContext(Dispatchers.IO) {
                try {
                    onProgress("Extracting JRE ZIP... This may take a minute.")
                    jreDir.mkdirs()
                    
                    val universalTar = File(jreDir.parentFile, "universal.tar.xz")
                    val binTar = File(jreDir.parentFile, "bin-arm64.tar.xz")
                    
                    ZipInputStream(FileInputStream(zipFile)).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            if (entry.name == "universal.tar.xz") {
                                FileOutputStream(universalTar).use { zis.copyTo(it) }
                            } else if (entry.name == "bin-arm64.tar.xz") {
                                FileOutputStream(binTar).use { zis.copyTo(it) }
                            }
                            entry = zis.nextEntry
                        }
                    }
                    zipFile.delete()
                    
                    if (!universalTar.exists() || !binTar.exists()) {
                        onProgress("Missing required tar files in ZIP.")
                        universalTar.delete()
                        binTar.delete()
                        return@withContext false
                    }
                    
                    onProgress("Extracting Universal libraries...")
                    extractTarXz(universalTar, jreDir, onProgress)
                    universalTar.delete()
                    
                    onProgress("Extracting ARM64 binaries...")
                    extractTarXz(binTar, jreDir, onProgress)
                    binTar.delete()

                    // Promotion step: If the tarballs extracted into a subfolder (e.g., "jre/"), move everything up.
                    onProgress("Checking JRE structure...")
                    val checkLibDir = File(jreDir, "lib")
                    if (!checkLibDir.exists()) {
                        val subDirs = jreDir.listFiles { file -> file.isDirectory }
                        if (subDirs != null && subDirs.size == 1) {
                            val subDir = subDirs[0]
                            onProgress("Promoting files from ${subDir.name}...")
                            subDir.listFiles()?.forEach { file ->
                                val dest = File(jreDir, file.name)
                                file.renameTo(dest)
                            }
                            subDir.delete()
                        }
                    }

                    onProgress("Setting permissions...")
                    val binDir = File(jreDir, "bin")
                    binDir.listFiles()?.forEach { it.setExecutable(true, false) }
                    val libDir = File(jreDir, "lib")
                    libDir.walkTopDown().forEach { 
                        if (it.isFile && (it.name.endsWith(".so") || it.name.contains("exec"))) {
                            it.setExecutable(true, false)
                        }
                    }
                    
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    onProgress("Extraction failed: ${e.message}")
                    false
                }
            }
            
            val javaBin = File(jreDir, "bin/java")
            val libjli = File(jreDir, "lib/libjli.so")
            val libjliAlternate = File(jreDir, "lib/jli/libjli.so")
            val libjvm = File(jreDir, "lib/server/libjvm.so")
            val modules = File(jreDir, "lib/modules")
            
            if (extracted && javaBin.exists() && (libjli.exists() || libjliAlternate.exists()) && libjvm.exists() && modules.exists()) {
                javaBin.setExecutable(true, false)
                onProgress("JRE Setup Complete.")
                return true
            } else {
                onProgress("Validation failed: Required JRE files missing.")
                onProgress("Files status: java=${javaBin.exists()}, libjli=${libjli.exists() || libjliAlternate.exists()}, libjvm=${libjvm.exists()}, modules=${modules.exists()}")
                jreDir.deleteRecursively()
            }
        }
        return false
    }
    
    private fun extractTarXz(tarXzFile: File, destDir: File, onProgress: (String) -> Unit) {
        TarArchiveInputStream(XZCompressorInputStream(BufferedInputStream(FileInputStream(tarXzFile)))).use { tarIn ->
            var entry = tarIn.nextTarEntry
            while (entry != null) {
                val outFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else if (entry.isSymbolicLink) {
                    outFile.parentFile?.mkdirs()
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= 21) {
                            android.system.Os.symlink(entry.linkName, outFile.absolutePath)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        tarIn.copyTo(fos)
                    }
                    if ((entry.mode and 0b001_001_001) != 0 || entry.name.endsWith(".so")) {
                        outFile.setExecutable(true, false)
                    }
                    onProgress("Extracted: ${entry.name}")
                }
                entry = tarIn.nextTarEntry
            }
        }
    }

    private suspend fun downloadFile(url: String, destination: File, onProgress: (String) -> Unit, name: String, isJar: Boolean = false): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                onProgress("Connecting to download $name...")
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    onProgress("Download failed:\nHTTP ${response.code}")
                    onProgress("Reason:\nServer file was not downloaded correctly.")
                    if (destination.exists()) destination.delete()
                    return@withContext false
                }
                val contentType = response.header("Content-Type")
                if (isJar && contentType != null && contentType.contains("text/html")) {
                    onProgress("Download failed:\nReceived HTML page instead of a JAR.")
                    onProgress("Reason:\nServer file was not downloaded correctly.")
                    if (destination.exists()) destination.delete()
                    return@withContext false
                }
                val body = response.body ?: return@withContext false
                val totalLength = body.contentLength()
                val inputStream = body.byteStream()
                destination.parentFile?.mkdirs()
                val outputStream = java.io.FileOutputStream(destination)
                val buffer = ByteArray(32 * 1024)
                var bytesRead: Int
                var downloaded: Long = 0
                onProgress("Downloading $name (0%)")
                var lastUpdate = System.currentTimeMillis()
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloaded += bytesRead
                    val now = System.currentTimeMillis()
                    if (now - lastUpdate > 500) {
                        lastUpdate = now
                        if (totalLength > 0) {
                            val progress = (downloaded * 100 / totalLength).toInt()
                            onProgress("Downloading $name ($progress%)")
                        } else {
                            val mb = downloaded / (1024.0 * 1024.0)
                            onProgress(String.format("Downloading $name... %.2f MB", mb))
                        }
                    }
                }
                outputStream.flush()
                outputStream.close()
                inputStream.close()
                if (isJar && destination.length() < 1024 * 1024) {
                    onProgress("Download failed: JAR file is less than 1MB. Likely corrupt.")
                    if (destination.exists()) destination.delete()
                    return@withContext false
                }
                onProgress("Download $name complete.")
                true
            } catch (e: Exception) {
                onProgress("Download error: ${e.message}")
                if (destination.exists()) destination.delete()
                false
            }
        }
    }
}
