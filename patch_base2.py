import re

with open('app/src/main/java/com/example/server/engine/BaseJavaEngine.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace jreDir logic to strictly use jre21
old_jre_def = """    val requiredJavaVersion: Int
        get() = if (serverJarName.contains("powernukkit", ignoreCase = true)) 21 else 17

    val requiredClassVersion: Double
        get() = if (requiredJavaVersion == 21) 65.0 else 61.0

    val jreDir: File
        get() = File(context.filesDir, "jre$requiredJavaVersion")"""

new_jre_def = """    val requiredJavaVersion: Int
        get() = if (serverJarName.contains("powernukkit", ignoreCase = true)) 21 else 21 // Always use Java 21!

    val requiredClassVersion: Double
        get() = if (serverJarName.contains("powernukkit", ignoreCase = true)) 65.0 else 61.0 // PNX requires 65.0

    val jreDir: File
        get() = File(context.filesDir, "jre21")"""

content = content.replace(old_jre_def, new_jre_def)

# Fix downloadAndExtractJre signature in BaseJavaEngine.kt
# Wait, I previously changed it to Downloader.downloadAndExtractJre(jreDir, requiredJavaVersion)
# Since we strictly use Java 21 now, we can just pass 21 or change Downloader back.
content = content.replace("Downloader.downloadAndExtractJre(jreDir, requiredJavaVersion)", "Downloader.downloadAndExtractJre(jreDir, 21)")

with open('app/src/main/java/com/example/server/engine/BaseJavaEngine.kt', 'w', encoding='utf-8') as f:
    f.write(content)
