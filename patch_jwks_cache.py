import re

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "r") as f:
    content = f.read()

target = """                    val code = authConn.responseCode
                    if (code == 200) {
                        val response = authConn.inputStream.bufferedReader().use { it.readText() }
                        if (response.contains("keys") || response.contains("kid")) {
                            jwksPass = true
                            jwtPass = true
                            
                            val jwksFile = java.io.File(serverDir, "jwks_cache.json")
                            jwksFile.writeText(response)
                        }
                    }
                } catch (e: Exception) {}"""

replacement = """                    val code = authConn.responseCode
                    if (code == 200) {
                        val response = authConn.inputStream.bufferedReader().use { it.readText() }
                        if (response.contains("keys") || response.contains("kid")) {
                            jwksPass = true
                            jwtPass = true
                            
                            val jwksFile = java.io.File(serverDir, "jwks_cache.json")
                            jwksFile.writeText(response)
                        }
                    }
                } catch (e: Exception) {
                    val jwksFile = java.io.File(serverDir, "jwks_cache.json")
                    if (jwksFile.exists() && jwksFile.length() > 0) {
                        if (System.currentTimeMillis() - jwksFile.lastModified() < 86400000) {
                            jwksPass = true
                            jwtPass = true
                        }
                    }
                }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "w") as f:
    f.write(content)
