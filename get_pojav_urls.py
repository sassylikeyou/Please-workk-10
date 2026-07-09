import urllib.request
import re

url = "https://raw.githubusercontent.com/PojavLauncherTeam/PojavLauncher/main/app/src/main/java/net/kdt/pojavlaunch/JreUtils.java"
try:
    with urllib.request.urlopen(url) as response:
        content = response.read().decode('utf-8')
        for line in content.split('\n'):
            if 'http' in line:
                print(line)
except Exception as e:
    print(e)
