import re

with open('app/src/main/java/com/example/server/engine/BaseJavaEngine.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Using regex to replace the block
pattern = re.compile(r'            try \{\s+val prefetchPath.*?launchServerProcess\(memoryMb\)\s+\}', re.DOTALL)

new_block = """            val diagPassed = com.example.server.NetworkDiagnosticsManager.runDiagnostics(context, serverDir, onLog)
            if (!diagPassed) {
                withContext(Dispatchers.Main) {
                    onLog("Diagnostics failed! Aborting server start.")
                    onStatusChange(com.example.server.ServerStatus.ERROR)
                }
                return@launch
            }
            startDiscoveryProxy()
            withContext(Dispatchers.Main) {
                launchServerProcess(memoryMb)
            }"""

if pattern.search(content):
    content = pattern.sub(new_block, content)
    print("Block patched.")
else:
    print("Block not found!")

with open('app/src/main/java/com/example/server/engine/BaseJavaEngine.kt', 'w', encoding='utf-8') as f:
    f.write(content)

