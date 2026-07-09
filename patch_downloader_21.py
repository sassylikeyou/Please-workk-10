import re

with open('app/src/main/java/com/example/server/Downloader.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    'private const val JRE17_URL = "https://github.com/MojoLauncher/android-openjdk-build-17-25/releases/download/rolling/jre17-pojav.zip"',
    ''
)
content = content.replace(
    'val jreUrl = if (version == 17) JRE17_URL else JRE21_URL',
    'val jreUrl = JRE21_URL'
)

with open('app/src/main/java/com/example/server/Downloader.kt', 'w', encoding='utf-8') as f:
    f.write(content)
