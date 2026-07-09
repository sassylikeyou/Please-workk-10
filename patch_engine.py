import re

with open('app/src/main/java/com/example/server/engine/BaseJavaEngine.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Modify LD_LIBRARY_PATH and add env vars
old_env = """                val ldLibPath = "${jreDir.absolutePath}/lib:${jreDir.absolutePath}/lib/jli:${jreDir.absolutePath}/lib/server"
                envMap["LD_LIBRARY_PATH"] = ldLibPath
                envMap["MALLOC_NANO_ZONE"] = "0" """

new_env = """                val ldLibPath = "/system/lib64:${jreDir.absolutePath}/lib:${jreDir.absolutePath}/lib/jli:${jreDir.absolutePath}/lib/server"
                envMap["LD_LIBRARY_PATH"] = ldLibPath
                envMap["_DISABLE_MTE_CHECKS"] = "1"
                envMap["MALLOC_CHECK_"] = "0"
                envMap["MALLOC_NANO_ZONE"] = "0" """

content = content.replace(old_env, new_env)
if "_DISABLE_MTE_CHECKS" not in content:
    print("Failed to replace env!")

# 2. Modify argsList
old_args = """                // Disable memory mapping for zips which can cause issues on some architectures
                argsList.add("-Dsun.zip.disableMemoryMapping=true")
                argsList.add("-Djna.nosys=true")
                argsList.add("-Dorg.jline.terminal.dumb=true")
                argsList.add("-Djline.terminal=jline.UnsupportedTerminal")
                
                argsList.add("-Xms512M")
                
                argsList.add("-Xmx1536M")"""

new_args = """                argsList.add("-Dorg.jline.terminal.dumb=true")
                argsList.add("-Djline.terminal=jline.UnsupportedTerminal")
                
                argsList.add("-Xms768M")
                
                argsList.add("-Xmx2048M")"""

content = content.replace(old_args, new_args)
if "768M" not in content:
    print("Failed to replace args!")

# 3. Add Java version diagnostics for 17
old_diag = """                        if (!versionOutput.contains("\\"21") && !versionOutput.contains("\\"22") && !versionOutput.contains("\\"23")) {
                            onLog("ERROR: Java version is not 21+. Aborting.")"""

new_diag = """                        if (!versionOutput.contains("\\"17") && !versionOutput.contains("\\"21") && !versionOutput.contains("\\"22") && !versionOutput.contains("\\"23")) {
                            onLog("ERROR: Java version is not 17+. Aborting.")"""

content = content.replace(old_diag, new_diag)
if "17+" not in content:
    print("Failed to replace diag!")

with open('app/src/main/java/com/example/server/engine/BaseJavaEngine.kt', 'w', encoding='utf-8') as f:
    f.write(content)
