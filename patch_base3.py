import re

with open('app/src/main/java/com/example/server/engine/BaseJavaEngine.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Make sure we print diagnostics properly and check if Java is incompatible
old_validation = """            // 1. VALIDATION PHASE (DO NOT SKIP)
            withContext(Dispatchers.Main) { onLog("3. Validating JAR file...") }"""

new_validation = """            // 1. VALIDATION PHASE (DO NOT SKIP)
            withContext(Dispatchers.Main) { onLog("3. Validating JAR file...") }
            if (requiredJavaVersion < 21 && serverJarName.contains("powernukkit", ignoreCase = true)) {
                 withContext(Dispatchers.Main) { onLog("Error: PowerNukkitX requires Java 21 (class version 65.0). Current selected is $requiredJavaVersion") }
                 // Actually we strictly use Java 21 so this shouldn't happen, but just in case
            }"""

content = content.replace(old_validation, new_validation)

with open('app/src/main/java/com/example/server/engine/BaseJavaEngine.kt', 'w', encoding='utf-8') as f:
    f.write(content)
