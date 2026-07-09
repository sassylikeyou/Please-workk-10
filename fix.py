with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "r") as f:
    text = f.read()

text = text.replace("\"stop\n\"", "\"stop\\n\"")

with open("app/src/main/java/com/example/server/engine/BaseJavaEngine.kt", "w") as f:
    f.write(text)
