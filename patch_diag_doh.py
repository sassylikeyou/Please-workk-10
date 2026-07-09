import re

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "r") as f:
    content = f.read()

target = """                try {
                    val address = java.net.InetAddress.getByName("authorization.franchise.minecraft-services.net")
                    dnsPass = address.hostAddress != null
                } catch (e: Exception) {}"""

replacement = """                try {
                    val address = java.net.InetAddress.getByName("authorization.franchise.minecraft-services.net")
                    dnsPass = address.hostAddress != null
                } catch (e: Exception) {
                    try {
                        val dohUrl = java.net.URL("https://dns.google/resolve?name=authorization.franchise.minecraft-services.net&type=A")
                        val conn = dohUrl.openConnection() as java.net.HttpURLConnection
                        if (conn.responseCode == 200) {
                            val json = conn.inputStream.bufferedReader().use { it.readText() }
                            dnsPass = json.contains("\"data\"")
                        }
                    } catch (e2: Exception) {}
                }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "w") as f:
    f.write(content)
