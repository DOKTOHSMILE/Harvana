#!/usr/bin/env python3
"""Writes app/module.toml from app/build.gradle: the same migration CodeAssist makes when it imports a Gradle module.

The two build files carry the same module. tools/module_toml_to_gradle.py goes one way, this script the other, so either
file can be edited (or lost: CodeAssist rewrites the Gradle files when it opens a project) and the other rebuilt from it.

    python3 tools/gradle_to_module_toml.py            # writes app/module.toml
    python3 tools/gradle_to_module_toml.py --check    # exits 1 if app/module.toml differs from what build.gradle gives

What carries over: namespace, SDK levels, version, build types, every dependency (Maven coordinates as they are,
files('libs/x.aar') as the file name, Maven twins of local files back to the local file), and any [android] key the
Gradle file keeps in its "// module.toml:" comment lines (for example kspProcessors). The source-set layout and the
[module] block are the standard CodeAssist ones; an existing app/module.toml keeps its own.
"""
import re
import sys
import tomllib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GRADLE = ROOT / "app" / "build.gradle"
OUT = ROOT / "app" / "module.toml"

# Maven coordinates the forward script writes for a local file in app/libs, read back as that file.
MAVEN_AS_LOCAL_FILE = {
    "androidx.core:core:1.13.0": "core-1.13.0.aar",
}
CONFIGS = ("implementation", "api", "compileOnly", "runtimeOnly", "testImplementation", "androidTestImplementation")

DEFAULT_HEAD = """version = 2

[module]
type = "android-app"
name = "app"
languageLevel = "JAVA_17"
output = "build/classes"
"""


def source_sets() -> str:
    out = []
    for kind in ("debug", "main", "release"):
        out.append(f"""[sourceSets.{kind}]
scope = "IMPLEMENTATION"
aidl = ["src/{kind}/aidl"]
assets = ["src/{kind}/assets"]
java = ["src/{kind}/java", "src/{kind}/kotlin"]
jniLibs = ["src/{kind}/jniLibs"]
res = ["src/{kind}/res"]
resources = ["src/{kind}/resources"]
""")
    return "\n".join(out)


def q(s: str) -> str:
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


def toml_list(items) -> str:
    return "[" + ", ".join(q(i) for i in items) + "]"


def block(text: str, name: str) -> str:
    """The body of the first `name { ... }` block (braces balanced)."""
    m = re.search(r"(^|\s)" + re.escape(name) + r"\s*\{", text)
    if not m:
        return ""
    i, depth = m.end(), 1
    start = i
    while i < len(text) and depth:
        depth += {"{": 1, "}": -1}.get(text[i], 0)
        i += 1
    return text[start:i - 1]


def value(text: str, key: str):
    m = re.search(r"^\s*" + key + r"\s*(?:=\s*)?[\"']?([^\"'\s]+)[\"']?\s*$", text, re.M)
    return m.group(1) if m else None


def dependencies(text: str) -> dict:
    deps = {c: [] for c in CONFIGS}
    body = block(text, "dependencies")
    for line in body.splitlines():
        line = line.split("//")[0].strip()
        m = re.match(r"(\w+)\s*\(?\s*files\(\s*['\"](?:libs/)?([^'\"]+)['\"]\s*\)\s*\)?$", line)
        if m and m.group(1) in deps:
            deps[m.group(1)].append(m.group(2))
            continue
        m = re.match(r"(\w+)\s*\(?\s*['\"]([^'\"]+)['\"]\s*\)?$", line)
        if m and m.group(1) in deps:
            coord = m.group(2)
            deps[m.group(1)].append(MAVEN_AS_LOCAL_FILE.get(coord, coord))
    return deps


def build_types(text: str) -> list:
    body = block(text, "buildTypes")
    out = []
    for m in re.finditer(r"(\w+)\s*\{([^{}]*)\}", body):
        name, inner = m.group(1), m.group(2)
        t = {"name": name}
        for key in ("debuggable", "minifyEnabled", "shrinkResources"):
            v = value(inner, key)
            if v in ("true", "false"):
                t[key] = v == "true"
        pf = re.search(r"proguardFiles\s+(.+)", inner)
        if pf:
            t["proguardFiles"] = re.findall(r"['\"]([^'\"]+)['\"]", pf.group(1))
        sc = re.search(r"signingConfig\s+signingConfigs\.(\w+)", inner)
        if sc and sc.group(1) != "debug":
            t["signingConfig"] = sc.group(1)
        out.append(t)
    return out


def build_type_toml(t: dict) -> str:
    parts = [f"name = {q(t['name'])}"]
    for key in ("debuggable", "minifyEnabled", "shrinkResources"):
        if key in t:
            parts.append(f"{key} = {str(t[key]).lower()}")
    if "proguardFiles" in t:
        parts.append(f"proguardFiles = {toml_list(t['proguardFiles'])}")
    if "signingConfig" in t:
        parts.append(f"signingConfig = {q(t['signingConfig'])}")
    return "{ " + ", ".join(parts) + " }"


def render() -> str:
    g = GRADLE.read_text()
    android = block(g, "android")
    dc = block(android, "defaultConfig")
    namespace = value(android, "namespace") or value(dc, "applicationId")
    a = {
        "namespace": namespace,
        "compileSdk": int(value(android, "compileSdk") or 34),
        "minSdk": int(value(dc, "minSdk") or 23),
        "targetSdk": int(value(dc, "targetSdk") or 34),
        "versionCode": int(value(dc, "versionCode") or 1),
        "versionName": value(dc, "versionName") or "1.0",
    }
    extras = re.findall(r"^// module\.toml: (.+)$", g, re.M)

    head = DEFAULT_HEAD
    sets = source_sets()
    if OUT.exists():   # keep this module's own [module] block and source sets
        old = OUT.read_text()
        cut = old.find("[dependencies]")
        if cut > 0:
            first = old.find("[sourceSets.")
            head, sets = (old[:first], old[first:cut]) if first > 0 else (old[:cut], "")
            head, sets = head.rstrip() + "\n", sets.rstrip() + "\n"

    deps = dependencies(g)
    dep_lines = [f"{c} = {toml_list(v)}" for c, v in deps.items() if v]
    bts = build_types(g)
    android_lines = [
        f"namespace = {q(a['namespace'])}",
        f"compileSdk = {a['compileSdk']}",
        f"minSdk = {a['minSdk']}",
        f"targetSdk = {a['targetSdk']}",
        'manifest = "src/main/AndroidManifest.xml"',
        f"versionCode = {a['versionCode']}",
        f"versionName = {q(a['versionName'])}",
        "isApplication = true",
        "manifestPlaceholders = []",
        "buildTypes = [" + ", ".join(build_type_toml(t) for t in bts) + "]",
        *extras,
    ]
    text = head + "\n" + sets + "\n[dependencies]\n" + "\n".join(dep_lines) + "\n\n[android]\n" + "\n".join(android_lines) + "\n"
    tomllib.loads(text)   # must parse
    return text


def main() -> int:
    if not GRADLE.exists():
        print("app/build.gradle is missing")
        return 1
    text = render()
    if "--check" in sys.argv:
        if not OUT.exists() or OUT.read_text() != text:
            print("app/module.toml is not what app/build.gradle gives. Run: python3 tools/gradle_to_module_toml.py")
            return 1
        return 0
    OUT.write_text(text)
    print(f"wrote {OUT.relative_to(ROOT)} from {GRADLE.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
