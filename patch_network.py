import re

with open('app/src/main/java/com/example/server/NetworkDiagnosticsManager.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = re.sub(r'        // Generate custom_hosts\.txt\n        generateHostsFile\(serverDir, domainDiscovery, ipDiscovery, domainAuth, ipAuth\)', '', content)
content = re.sub(r'    private fun generateHostsFile.*?\}', '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/server/NetworkDiagnosticsManager.kt', 'w', encoding='utf-8') as f:
    f.write(content)
