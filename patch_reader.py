import re

with open('app/src/main/java/com/example/server/engine/BaseJavaEngine.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("withContext(Dispatchers.Main) { \n                                onLog(line)", "scope.launch(Dispatchers.Main) { \n                                onLog(line)")

content = content.replace('withContext(Dispatchers.Main) { onLog("Xbox authentication state: VERIFIED") }', 'scope.launch(Dispatchers.Main) { onLog("Xbox authentication state: VERIFIED") }')
content = content.replace('withContext(Dispatchers.Main) { onLog("[JRE] Applying default language: eng") }', 'scope.launch(Dispatchers.Main) { onLog("[JRE] Applying default language: eng") }')
content = content.replace('withContext(Dispatchers.Main) { onLog("[JRE] Auto-accepting license") }', 'scope.launch(Dispatchers.Main) { onLog("[JRE] Auto-accepting license") }')
content = content.replace('''                            if (line.contains("UnknownHostException: authorization.franchise.minecraft-services.net") || line.contains("InvalidJwtException")) {
                                withContext(Dispatchers.Main) {
                                    onLog("Xbox authentication service unavailable.")
                                    onLog("Player authentication failed.")
                                }
                            }''', '''                            if (line.contains("UnknownHostException: authorization.franchise.minecraft-services.net") || line.contains("InvalidJwtException")) {
                                scope.launch(Dispatchers.Main) {
                                    onLog("Xbox authentication service unavailable.")
                                    onLog("Player authentication failed.")
                                }
                            }''')

with open('app/src/main/java/com/example/server/engine/BaseJavaEngine.kt', 'w', encoding='utf-8') as f:
    f.write(content)
