import re

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "r") as f:
    content = f.read()

target = """                for (domain in domainsToResolve) {
                    try {
                        val addresses = java.net.InetAddress.getAllByName(domain)
                        for (addr in addresses) {
                            hostsContent.append("${addr.hostAddress} $domain\\n")
                        }
                    } catch (e: Exception) {}
                }"""

replacement = """                for (domain in domainsToResolve) {
                    try {
                        val addresses = java.net.InetAddress.getAllByName(domain)
                        for (addr in addresses) {
                            hostsContent.append("${addr.hostAddress} $domain\\n")
                        }
                    } catch (e: Exception) {
                        try {
                            val dohUrl = java.net.URL("https://dns.google/resolve?name=$domain&type=A")
                            val conn = dohUrl.openConnection() as java.net.HttpURLConnection
                            if (conn.responseCode == 200) {
                                val json = conn.inputStream.bufferedReader().use { it.readText() }
                                val regex = "\"data\":\\s*\"([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)\"".toRegex()
                                val matches = regex.findAll(json)
                                for (match in matches) {
                                    hostsContent.append("${match.groupValues[1]} $domain\\n")
                                }
                            }
                        } catch (e2: Exception) {}
                        
                        try {
                            val dohUrl2 = java.net.URL("https://cloudflare-dns.com/dns-query?name=$domain&type=A")
                            val conn2 = dohUrl2.openConnection() as java.net.HttpURLConnection
                            conn2.setRequestProperty("accept", "application/dns-json")
                            if (conn2.responseCode == 200) {
                                val json = conn2.inputStream.bufferedReader().use { it.readText() }
                                val regex = "\"data\":\\s*\"([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)\"".toRegex()
                                val matches = regex.findAll(json)
                                for (match in matches) {
                                    hostsContent.append("${match.groupValues[1]} $domain\\n")
                                }
                            }
                        } catch (e3: Exception) {}
                    }
                }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "w") as f:
    f.write(content)
