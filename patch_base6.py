import re

with open('app/src/main/java/com/example/server/engine/BaseJavaEngine.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_ver_chk = """                        if (!versionOutput.contains("\\"17") && !versionOutput.contains("\\"21") && !versionOutput.contains("\\"22") && !versionOutput.contains("\\"23")) {
                            onLog("ERROR: Java version is not 17+. Aborting.")"""

new_ver_chk = """                        if (!versionOutput.contains("\\"21") && !versionOutput.contains("\\"22") && !versionOutput.contains("\\"23")) {
                            onLog("ERROR: Java version is not 21+. Aborting.")"""

content = content.replace(old_ver_chk, new_ver_chk)

with open('app/src/main/java/com/example/server/engine/BaseJavaEngine.kt', 'w', encoding='utf-8') as f:
    f.write(content)
