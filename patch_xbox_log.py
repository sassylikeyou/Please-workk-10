import re

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "r") as f:
    content = f.read()

target = """                if (code == 200) {
                    withContext(Dispatchers.Main) { onLog("Internet authentication available.") }
                } else {
                    withContext(Dispatchers.Main) { onLog("Internet authentication unavailable (Code: $code)") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onLog("Internet authentication unavailable") }
            }"""

replacement = """                if (code == 200) {
                    withContext(Dispatchers.Main) { onLog("Internet authentication available.") }
                } else {
                    withContext(Dispatchers.Main) { 
                        onLog("Xbox authentication service unavailable.")
                        onLog("Players may fail authentication.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { 
                    onLog("Xbox authentication service unavailable.")
                    onLog("Players may fail authentication.")
                }
            }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "w") as f:
    f.write(content)
