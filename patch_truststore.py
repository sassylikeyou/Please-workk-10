import re

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "r") as f:
    content = f.read()

target = """                argsList.add("-Dhttps.protocols=TLSv1.2,TLSv1.3")"""

replacement = """                argsList.add("-Dhttps.protocols=TLSv1.2,TLSv1.3")
                
                val trustStoreFile = java.io.File(serverDir, "truststore.p12")
                if (!trustStoreFile.exists()) {
                    try {
                        val keyStore = java.security.KeyStore.getInstance("AndroidCAStore")
                        keyStore.load(null, null)
                        
                        val p12 = java.security.KeyStore.getInstance("PKCS12")
                        p12.load(null, null)
                        
                        val aliases = keyStore.aliases()
                        while (aliases.hasMoreElements()) {
                            val alias = aliases.nextElement()
                            val cert = keyStore.getCertificate(alias)
                            p12.setCertificateEntry(alias, cert)
                        }
                        
                        java.io.FileOutputStream(trustStoreFile).use { fos ->
                            p12.store(fos, "changeit".toCharArray())
                        }
                    } catch (e: Exception) {
                        onLog("Failed to create truststore: ${e.message}")
                    }
                }
                argsList.add("-Djavax.net.ssl.trustStore=${trustStoreFile.absolutePath}")
                argsList.add("-Djavax.net.ssl.trustStorePassword=changeit")
                argsList.add("-Djavax.net.ssl.trustStoreType=PKCS12")"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "w") as f:
    f.write(content)
