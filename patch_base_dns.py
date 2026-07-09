import re

with open('app/src/main/java/com/example/server/engine/BaseJavaEngine.kt', 'r', encoding='utf-8') as f:
    content = f.read()

pattern = re.compile(r'                val domainsToResolve = listOf\(.*?argsList\.add\("-Dhttps\.protocols=TLSv1\.2,TLSv1\.3"\)', re.DOTALL)

if pattern.search(content):
    content = pattern.sub('                // DNS resolution moved to NetworkDiagnosticsManager', content)
    print("DNS block removed.")
else:
    print("DNS block not found.")

with open('app/src/main/java/com/example/server/engine/BaseJavaEngine.kt', 'w', encoding='utf-8') as f:
    f.write(content)
