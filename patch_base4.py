import re

with open('app/src/main/java/com/example/server/engine/BaseJavaEngine.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_args = """                argsList.add("-Xms768M")
                
                argsList.add("-Xmx2048M")"""

new_args = """                argsList.add("-Xms512M")
                argsList.add("-Xmx1536M")
                argsList.add("-XX:+UseSerialGC")
                argsList.add("-XX:-UseCompressedOops")
                argsList.add("-Djava.awt.headless=true")"""

content = content.replace(old_args, new_args)

old_diag = """                try {
                    val diagPb = ProcessBuilder(javaBin.absolutePath, "-version")"""

new_diag = """                try {
                    val diagPbX = ProcessBuilder(javaBin.absolutePath, "-XshowSettings:vm", "-version")
                    diagPbX.directory(serverDir)
                    diagPbX.environment().putAll(envMap)
                    diagPbX.environment().remove("JAVA_TOOL_OPTIONS")
                    diagPbX.environment().remove("_JAVA_OPTIONS")
                    val pDiagX = diagPbX.start()
                    val readerDiagX = java.io.BufferedReader(java.io.InputStreamReader(pDiagX.inputStream))
                    val errorDiagX = java.io.BufferedReader(java.io.InputStreamReader(pDiagX.errorStream))
                    var lineDiagX: String?
                    while (readerDiagX.readLine().also { lineDiagX = it } != null) {
                        withContext(Dispatchers.Main) { onLog("java settings stdout: $lineDiagX") }
                    }
                    while (errorDiagX.readLine().also { lineDiagX = it } != null) {
                        withContext(Dispatchers.Main) { onLog("java settings stderr: $lineDiagX") }
                    }
                    pDiagX.waitFor()

                    val diagPb = ProcessBuilder(javaBin.absolutePath, "-version")"""

content = content.replace(old_diag, new_diag)

with open('app/src/main/java/com/example/server/engine/BaseJavaEngine.kt', 'w', encoding='utf-8') as f:
    f.write(content)
