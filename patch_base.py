import re

with open('app/src/main/java/com/example/server/engine/BaseJavaEngine.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Replace jreDir definition
old_jre_def = """    val jreDir: File
        get() = File(context.filesDir, "jre21")"""

new_jre_def = """    val requiredJavaVersion: Int
        get() = if (serverJarName.contains("powernukkit", ignoreCase = true)) 21 else 17

    val requiredClassVersion: Double
        get() = if (requiredJavaVersion == 21) 65.0 else 61.0

    val jreDir: File
        get() = File(context.filesDir, "jre$requiredJavaVersion")"""

content = content.replace(old_jre_def, new_jre_def)

# 2. Update Downloader.downloadAndExtractJre
old_dl = "Downloader.downloadAndExtractJre(jreDir) { progress ->"
new_dl = "Downloader.downloadAndExtractJre(jreDir, requiredJavaVersion) { progress ->"
content = content.replace(old_dl, new_dl)

# 3. Add diagnostics print in startServer
# Let's find where to add it. "--- Server Startup Diagnostics ---" is around line 433
old_diag_print = """                withContext(Dispatchers.Main) {
                    onLog("--- Server Startup Diagnostics ---")
                    onLog("Java executable path: ${javaBin.absolutePath}")
                    onLog("Java architecture: ${android.os.Build.SUPPORTED_ABIS.joinToString(", ")}")
                    onLog("Server jar path: ${serverJar.absolutePath}")
                    onLog("Server jar size: ${serverJar.length()} bytes")
                    onLog("Working directory: ${serverDir.absolutePath}")
                    onLog("Temporary directory: ${tmpDir.absolutePath}")
                    onLog("LD_LIBRARY_PATH: ${jreDir.absolutePath}/lib:${jreDir.absolutePath}/lib/jli:${jreDir.absolutePath}/lib/server")"""

new_diag_print = """                withContext(Dispatchers.Main) {
                    onLog("--- Server Startup Diagnostics ---")
                    onLog("Detected class version requirement: $requiredClassVersion")
                    onLog("Selected Java version: $requiredJavaVersion")
                    onLog("JAVA_HOME: ${jreDir.absolutePath}")
                    onLog("Java executable path: ${javaBin.absolutePath}")
                    onLog("Java architecture: ${android.os.Build.SUPPORTED_ABIS.joinToString(", ")}")
                    onLog("Server jar path: ${serverJar.absolutePath}")
                    onLog("Server jar size: ${serverJar.length()} bytes")
                    onLog("Working directory: ${serverDir.absolutePath}")
                    onLog("Temporary directory: ${tmpDir.absolutePath}")
                    onLog("LD_LIBRARY_PATH: /system/lib64:${jreDir.absolutePath}/lib:${jreDir.absolutePath}/lib/server:${jreDir.absolutePath}/lib/jli")"""

content = content.replace(old_diag_print, new_diag_print)

# 4. Check LD_LIBRARY_PATH env var
old_ld = """                val ldLibPath = "/system/lib64:${jreDir.absolutePath}/lib:${jreDir.absolutePath}/lib/jli:${jreDir.absolutePath}/lib/server\""""
new_ld = """                val ldLibPath = "/system/lib64:${jreDir.absolutePath}/lib:${jreDir.absolutePath}/lib/server:${jreDir.absolutePath}/lib/jli\""""
content = content.replace(old_ld, new_ld)


with open('app/src/main/java/com/example/server/engine/BaseJavaEngine.kt', 'w', encoding='utf-8') as f:
    f.write(content)
