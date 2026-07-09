import re

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "r") as f:
    content = f.read()

target = """                withContext(Dispatchers.Main) { onLog("Initializing Java process...") }"""
replacement = """                withContext(Dispatchers.Main) { 
                    onLog("Xbox Authentication:\\nENABLE")
                    onLog("Initializing Java process...") 
                }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "w") as f:
    f.write(content)
