import re

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "r") as f:
    content = f.read()

content = content.replace("json.contains(\"\"data\"\")", "json.contains(\"\\\"data\\\"\")")

target_regex_1 = """val regex = "\\"data\\":\\\\s*\\"([0-9]+\\\\.[0-9]+\\\\.[0-9]+\\\\.[0-9]+)\\"".toRegex()"""
replacement_regex = """val regex = \"\"\""data":\\s*"([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)"\"\"\".toRegex()"""

content = content.replace(target_regex_1, replacement_regex)

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "w") as f:
    f.write(content)
