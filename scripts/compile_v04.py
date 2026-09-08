"""用 Python 直接调用 K2JVMCompiler 编译 v0.4 测试。
避免 PowerShell 对分号/路径的解析问题。"""
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(r"C:\Users\yuwen\Desktop\精品神药")
GRADLE_CACHE = Path(r"C:\Users\yuwen\.gradle\caches\modules-2\files-2.1")

def find_jar(*subpath_components):
    """在 gradle cache 里找指定 jar (e.g. find_jar('org.jetbrains.kotlin', 'kotlin-stdlib', '2.2.10'))"""
    base = GRADLE_CACHE.joinpath(*subpath_components)
    return str(next(base.rglob("*.jar")))

KC = find_jar("org.jetbrains.kotlin", "kotlin-compiler-embeddable", "2.2.10")
KSTDLIB = find_jar("org.jetbrains.kotlin", "kotlin-stdlib", "2.2.10")
KREFLECT = find_jar("org.jetbrains.kotlin", "kotlin-reflect", "2.2.10")
KJSON = find_jar("org.jetbrains.kotlinx", "kotlinx-serialization-json-jvm", "1.7.3")
KCORE = find_jar("org.jetbrains.kotlinx", "kotlinx-serialization-core-jvm", "1.7.3")
KCOROUTINES = find_jar("org.jetbrains.kotlinx", "kotlinx-coroutines-core-jvm", "1.9.0")
ANDROID_JAR = r"C:\Users\yuwen\AppData\Local\Android\Sdk\platforms\android-36\android.jar"

# 编译器 classpath 包含它自己 + 它运行时的依赖
COMPILER_CP = ";".join([KC, KSTDLIB, KREFLECT, KCOROUTINES])
# 编译产物的 classpath (应用代码的 deps + android.jar)
APP_CP = ";".join([KSTDLIB, KJSON, KCORE, ANDROID_JAR])

OUT_MAIN = ROOT / "app" / "build" / "classes" / "main_v04"
OUT_TEST = ROOT / "app" / "build" / "classes" / "test_v04"
OUT_MAIN.mkdir(parents=True, exist_ok=True)
OUT_TEST.mkdir(parents=True, exist_ok=True)

# 1. 收集 main 源码
main_files = list((ROOT / "app" / "src" / "main" / "java").rglob("*.kt"))
print(f"[main] {len(main_files)} files -> {OUT_MAIN}")

# 2. 编译 main
cmd_main = [
    "C:\\Users\\yuwen\\jdk-17\\bin\\java.exe",
    "-cp", COMPILER_CP,
    "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
    "-d", str(OUT_MAIN),
    "-classpath", APP_CP,
] + [str(f) for f in main_files]

print("=== compile main ===")
r = subprocess.run(cmd_main, capture_output=True, text=True, encoding="utf-8", errors="replace")
# 写到文件以避免 GBK 编码问题
(ROOT / "compile-main.stdout.log").write_text(r.stdout, encoding="utf-8")
(ROOT / "compile-main.stderr.log").write_text(r.stderr, encoding="utf-8")
print(f"exit: {r.returncode}")
print(f"stdout: {len(r.stdout)} chars -> compile-main.stdout.log")
print(f"stderr: {len(r.stderr)} chars -> compile-main.stderr.log")

if r.returncode != 0:
    print("[FAIL] main compile failed")
    sys.exit(1)

# 3. 检查 .class 是否生成
classes = list(OUT_MAIN.rglob("*.class"))
print(f"[main] generated {len(classes)} class files")
