import re

with open('app/src/main/java/com/example/server/Downloader.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Change JRE_URL to a function that takes version
content = content.replace('private const val JRE_URL = "https://github.com/MojoLauncher/android-openjdk-build-17-25/releases/download/rolling/jre17-pojav.zip"', 
                          'private const val JRE17_URL = "https://github.com/MojoLauncher/android-openjdk-build-17-25/releases/download/rolling/jre17-pojav.zip"\n    private const val JRE21_URL = "https://github.com/MojoLauncher/android-openjdk-build-21-25/releases/download/rolling/jre21-pojav.zip"')

# Update downloadAndExtractJre signature and logic
old_download = """    suspend fun downloadAndExtractJre(jreDir: File, onProgress: (String) -> Unit): Boolean {
        var attempts = 0
        while (attempts < 2) {
            attempts++
            if (attempts > 1) {
                onProgress("Retrying JRE setup (Attempt $attempts/2)...")
                jreDir.deleteRecursively()
            }
            
            val zipFile = File(jreDir.parentFile, "jre.zip")
            onProgress("JRE download URL: $JRE_URL")
            val success = downloadFile(JRE_URL, zipFile, onProgress, "JRE")"""

new_download = """    suspend fun downloadAndExtractJre(jreDir: File, version: Int, onProgress: (String) -> Unit): Boolean {
        val jreUrl = if (version == 17) JRE17_URL else JRE21_URL
        var attempts = 0
        while (attempts < 2) {
            attempts++
            if (attempts > 1) {
                onProgress("Retrying JRE setup (Attempt $attempts/2)...")
                jreDir.deleteRecursively()
            }
            
            val zipFile = File(jreDir.parentFile, "jre${version}.zip")
            onProgress("JRE download URL: $jreUrl")
            val success = downloadFile(jreUrl, zipFile, onProgress, "JRE")"""

content = content.replace(old_download, new_download)

with open('app/src/main/java/com/example/server/Downloader.kt', 'w', encoding='utf-8') as f:
    f.write(content)
