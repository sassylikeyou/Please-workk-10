import re

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "r") as f:
    content = f.read()

target = """                argsList.add("-Xmx1024M")"""

replacement = """                argsList.add("-Xmx1024M")
                
                val domainsToResolve = listOf(
                    "authorization.franchise.minecraft-services.net",
                    "api.minecraftservices.com",
                    "sessionserver.mojang.com",
                    "api.mojang.com",
                    "textures.minecraft.net"
                )
                val hostsContent = java.lang.StringBuilder()
                for (domain in domainsToResolve) {
                    try {
                        val addresses = java.net.InetAddress.getAllByName(domain)
                        for (addr in addresses) {
                            hostsContent.append("${addr.hostAddress} $domain\\n")
                        }
                    } catch (e: Exception) {}
                }
                val customHostsFile = java.io.File(serverDir, "custom_hosts.txt")
                customHostsFile.writeText(hostsContent.toString())
                argsList.add("-Djdk.net.hosts.file=${customHostsFile.absolutePath}")
                argsList.add("-Dhttps.protocols=TLSv1.2,TLSv1.3")"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "w") as f:
    f.write(content)
