import re

with open('app/src/main/java/com/example/server/engine/CloudburstEngine.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    'override val serverJarUrl = "https://github.com/PetteriM1/NukkitPetteriM1Edition/releases/download/4437/Nukkit-PM1E.jar"',
    'override val serverJarUrl = "https://repo.opencollab.dev/maven-snapshots/cn/nukkit/nukkit/1.0-SNAPSHOT/nukkit-1.0-20260616.184029-1239.jar"'
)

with open('app/src/main/java/com/example/server/engine/CloudburstEngine.kt', 'w', encoding='utf-8') as f:
    f.write(content)
