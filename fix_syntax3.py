import re

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "r") as f:
    content = f.read()

content = content.replace("Regex(\"\\\"data\\\":\s*\\\"([0-9]+\.[0-9]+\.[0-9]+\.[0-9]+)\\\"\")", "Regex(\"\\\"data\\\":\\\\s*\\\"([0-9]+\\\\.[0-9]+\\\\.[0-9]+\\\\.[0-9]+)\\\"\")")

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "w") as f:
    f.write(content)
