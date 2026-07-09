import re

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "r") as f:
    content = f.read()

old_code = """                val serverProps = File(serverDir, "server.properties")
                if (!serverProps.exists()) {
                    serverProps.writeText(
                        "server-ip=0.0.0.0\\n" +
                        "server-port=19132\\n" +
                        "xbox-auth=false\\n"
                    )
                }"""

new_code = """                val serverProps = File(serverDir, "server.properties")
                if (!serverProps.exists()) {
                    serverProps.writeText(
                        "server-ip=0.0.0.0\\n" +
                        "server-port=19132\\n" +
                        "xbox-auth=on\\n"
                    )
                } else {
                    var propsContent = serverProps.readText()
                    var changed = false
                    if (propsContent.contains("xbox-auth=off")) {
                        propsContent = propsContent.replace("xbox-auth=off", "xbox-auth=on")
                        changed = true
                    }
                    if (propsContent.contains("xbox-auth=false")) {
                        propsContent = propsContent.replace("xbox-auth=false", "xbox-auth=on")
                        changed = true
                    }
                    if (changed) {
                        serverProps.writeText(propsContent)
                    }
                }"""

if old_code in content:
    content = content.replace(old_code, new_code)
    with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "w") as f:
        f.write(content)
    print("Patched successfully")
else:
    print("Old code not found")
