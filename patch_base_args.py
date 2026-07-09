with open('app/src/main/java/com/example/server/engine/BaseJavaEngine.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('argsList.add("-Djdk.net.hosts.file=custom_hosts.txt")', 'argsList.add("-Djdk.net.hosts.file=${File(serverDir, "custom_hosts.txt").absolutePath}")')

with open('app/src/main/java/com/example/server/engine/BaseJavaEngine.kt', 'w', encoding='utf-8') as f:
    f.write(content)
