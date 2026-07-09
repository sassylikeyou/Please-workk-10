import re

with open('app/src/main/java/com/example/server/Downloader.kt', 'r', encoding='utf-8') as f:
    content = f.read()

old_url = '"https://github.com/MojoLauncher/android-openjdk-build-17-25/releases/download/rolling/jre21-pojav.zip"'
new_url = '"https://github.com/awoot6549/android-openjdk-build-multiarch/releases/download/rolling/jre21-pojav.zip"'

content = content.replace(old_url, new_url)

with open('app/src/main/java/com/example/server/Downloader.kt', 'w', encoding='utf-8') as f:
    f.write(content)
