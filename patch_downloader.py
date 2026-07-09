import re

with open('app/src/main/java/com/example/server/Downloader.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Add removal of unnecessary desktop Java native libraries
old_extract = """                    extractTarXz(binTar, jreDir, onProgress)
                    binTar.delete()"""

new_extract = """                    extractTarXz(binTar, jreDir, onProgress)
                    binTar.delete()
                    
                    onProgress("Removing desktop UI libraries...")
                    val libDirForCleanup = File(jreDir, "lib")
                    File(libDirForCleanup, "libawt.so").delete()
                    File(libDirForCleanup, "libawt_headless.so").delete()
                    File(libDirForCleanup, "libfontmanager.so").delete()"""

content = content.replace(old_extract, new_extract)

with open('app/src/main/java/com/example/server/Downloader.kt', 'w', encoding='utf-8') as f:
    f.write(content)
