import re

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "r") as f:
    content = f.read()

# Remove the startDiscoveryProxy call
content = content.replace("startDiscoveryProxy()", "// startDiscoveryProxy() removed")

# Remove the proxy modifications in the download / cache code
# The code was:
# responseBody = responseBody.replace("https://client.discovery.minecraft-services.net", "http://127.0.0.1:19133")
content = content.replace('responseBody = responseBody.replace("https://client.discovery.minecraft-services.net", "http://127.0.0.1:19133")',
                          '// proxy replacement removed')

# The offlineData was also hardcoded with 127.0.0.1:19133
old_offline = 'val offlineData = "{\\"TenantId\\":\\"placeholder\\",\\"SigningKeys\\":[],\\"SpringboardUrl\\":\\"http://127.0.0.1:19133/v1/springboard\\",\\"MinecraftServicesUrl\\":\\"http://127.0.0.1:19133\\",\\"MinecraftServicesDiscoveryUrl\\":\\"http://127.0.0.1:19133\\"}"'
new_offline = 'val offlineData = "{\\"TenantId\\":\\"placeholder\\",\\"SigningKeys\\":[],\\"SpringboardUrl\\":\\"https://client.discovery.minecraft-services.net/v1/springboard\\",\\"MinecraftServicesUrl\\":\\"https://client.discovery.minecraft-services.net\\",\\"MinecraftServicesDiscoveryUrl\\":\\"https://client.discovery.minecraft-services.net\\"}"'
content = content.replace(old_offline, new_offline)

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "w") as f:
    f.write(content)
