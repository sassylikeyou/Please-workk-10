import re

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "r") as f:
    content = f.read()

target = """                // Dynamic memory allocation based on available RAM
                val safeRam = when {
                    availRamMb > 3000 -> 2048
                    availRamMb >= 1500 -> 1024
                    else -> 768
                }
                argsList.add("-Xmx${safeRam}M")"""
replacement = """                argsList.add("-Xmx1024M")"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "w") as f:
    f.write(content)
