import re

with open("app/src/main/java/com/example/server/health/ServerHealthMonitor.kt", "r") as f:
    content = f.read()

target = """    fun analyzeLogLine(line: String) {
        if (currentStatus == ServerStatus.STARTING) {
            if (ServerStartupPatterns.isSuccess(line)) {
                // Potential success, wait a short verification period to see if it crashes immediately
                if (verificationJob == null || verificationJob?.isActive != true) {
                    verificationJob = scope.launch {
                        delay(1000) // 1 second verification
                        if (currentStatus == ServerStatus.STARTING) {
                            onLog("SERVER READY STATE: Online!")
                            setStatus(ServerStatus.RUNNING)
                        }
                    }
                }
            }
        }
    }"""
replacement = """    fun analyzeLogLine(line: String) {
        if (line.contains("libutil.so.1") || line.contains("jlinenative") || line.contains("Advanced terminal features unavailable")) {
            // These are Android compatibility warnings, ignore them.
        }
        
        if (currentStatus == ServerStatus.STARTING) {
            if (ServerStartupPatterns.isSuccess(line)) {
                // Potential success, wait a short verification period to see if it crashes immediately
                if (verificationJob == null || verificationJob?.isActive != true) {
                    verificationJob = scope.launch {
                        delay(1000) // 1 second verification
                        if (currentStatus == ServerStatus.STARTING) {
                            onLog("SERVER READY STATE: Online!")
                            setStatus(ServerStatus.RUNNING)
                        }
                    }
                }
            }
        }
    }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/server/health/ServerHealthMonitor.kt", "w") as f:
    f.write(content)
