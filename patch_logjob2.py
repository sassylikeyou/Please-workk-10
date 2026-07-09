import re

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "r") as f:
    content = f.read()

target = """                                if (line.contains("UDP socket") || line.contains("RakNet")) {
                                    Log.i("ServerManager", "[DEBUG] UDP socket opened successfully: $line")
                                }"""
replacement = """                                if (line.contains("UDP socket") || line.contains("RakNet")) {
                                    Log.i("ServerManager", "[DEBUG] UDP socket opened successfully: $line")
                                }
                                if (line.contains("UnknownHostException: authorization.franchise.minecraft-services.net") || line.contains("InvalidJwtException")) {
                                    onLog("Xbox authentication service unavailable.")
                                    onLog("Player authentication failed.")
                                }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "w") as f:
    f.write(content)
