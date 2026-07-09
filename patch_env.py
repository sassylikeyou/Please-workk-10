import re

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "r") as f:
    content = f.read()

content = content.replace("diagPb.environment().clear()", "// diagPb.environment().clear()")
content = content.replace("pb.environment().clear()", "// pb.environment().clear()")

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "r") as f:
    content = f.read()

target = """                pb.environment().putAll(envMap)"""
replacement = """                pb.environment().putAll(envMap)
                pb.environment().remove("JAVA_TOOL_OPTIONS")
                pb.environment().remove("_JAVA_OPTIONS")
                pb.environment().remove("http_proxy")
                pb.environment().remove("https_proxy")
                pb.environment().remove("HTTP_PROXY")
                pb.environment().remove("HTTPS_PROXY")"""

content = content.replace(target, replacement)

target_diag = """                    diagPb.environment().putAll(envMap)"""
replacement_diag = """                    diagPb.environment().putAll(envMap)
                    diagPb.environment().remove("JAVA_TOOL_OPTIONS")
                    diagPb.environment().remove("_JAVA_OPTIONS")"""

content = content.replace(target_diag, replacement_diag)

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "w") as f:
    f.write(content)
