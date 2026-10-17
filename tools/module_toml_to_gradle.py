#!/usr/bin/env python3
"""Writes app/build.gradle from app/module.toml, so the CodeAssist build file is the one source of truth.

CodeAssist builds from app/module.toml. GitHub Actions builds with Gradle. To stop the two drifting apart, CI runs this
script first: the module's identity, SDK levels, build types and every dependency come from the toml; the few things the toml has
no place for (plugins, the fixed debug key, lint and Java level) are written here.

    python3 tools/module_toml_to_gradle.py            # writes app/build.gradle
    python3 tools/module_toml_to_gradle.py --check    # exits 1 if app/build.gradle is not what the toml produces
"""
import sys
import tomllib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TOML = ROOT / "app" / "module.toml"
OUT = ROOT / "app" / "build.gradle"


# CodeAssist deletes the root Gradle files when it opens a project, and a commit made from the phone can push those deletions.
# CI would then fail within seconds, so this puts back any that are missing (never overwrites one that exists).
ROOT_FILES = {
    "build.gradle": "// Root build file managed via settings repositories\n",
    "settings.gradle": """pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}

rootProject.name = "Harvana"
include ':app'
""",
    "gradle.properties": "android.useAndroidX=true\nandroid.enableJetifier=true\norg.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8\n",
}


def restore_root_files():
    for name, body in ROOT_FILES.items():
        f = ROOT / name
        if not f.exists():
            f.write_text(body)
            print("restored missing", name)


# CodeAssist uses these local files. Gradle must not: a local .aar next to the same library arriving through another
# dependency (appcompat needs core) is a duplicate-class error. So CI builds the same version from Maven.
LOCAL_FILE_AS_MAVEN = {
    "core-1.13.0.aar": "androidx.core:core:1.13.0",
    "material-1.11.0.aar": "com.google.android.material:material:1.11.0",
}


def dependency(line: str, config: str) -> str:
    """A maven coordinate stays a coordinate. A known local file becomes its Maven twin; any other file is used from app/libs."""
    name = line.strip()
    if name in LOCAL_FILE_AS_MAVEN:
        return f"    {config} '{LOCAL_FILE_AS_MAVEN[name]}'"
    if name.endswith((".aar", ".jar")):
        return f"    {config} files('libs/{name}')"
    return f"    {config} '{name}'"


def build_types(types) -> str:
    out = []
    for t in types:
        name = t["name"]
        lines = [f"        {name} {{"]
        if "debuggable" in t:
            lines.append(f"            debuggable {str(bool(t['debuggable'])).lower()}")
        if "minifyEnabled" in t:
            lines.append(f"            minifyEnabled {str(bool(t['minifyEnabled'])).lower()}")
        if "shrinkResources" in t:
            lines.append(f"            shrinkResources {str(bool(t['shrinkResources'])).lower()}")
        if "signingConfig" in t:
            lines.append(f"            signingConfig signingConfigs.{t['signingConfig']}")
        files = t.get("proguardFiles", [])
        if files:
            args = ", ".join(f"'{f}'" if f != "proguard-android-optimize.txt" else "getDefaultProguardFile('proguard-android-optimize.txt')" for f in files)
            lines.append(f"            proguardFiles {args}")
        lines.append("        }")
        out.append("\n".join(lines))
    return "\n".join(out)


def release_signing_configs(types) -> str:
    """Emit a signingConfigs block for every non-debug signing config referenced by a build type.

    The keystore and its passwords are not in the repo: the .keystore file is dropped into app/ by the
    build host, and the credentials are read at build time from a Gradle property (local
    ~/.gradle/gradle.properties) or an environment variable (CI secrets). Using the env var path
    means the credentials never enter git.
    """
    blocks = []
    seen = set()
    for t in types:
        name = t.get("signingConfig")
        if not name or name == "debug" or name in seen:
            continue
        seen.add(name)
        env = name.upper()
        blocks.append(f"""        // Release signing config. The keystore and its passwords are not in the repo - the
        // store file is dropped into app/ by the build host, and the credentials come from
        // environment variables (or a local gradle.properties that is never committed). For
        // local dry-runs set them in ~/.gradle/gradle.properties; for CI set them in the job
        // secrets so they never end up in git.
        {name} {{
            storeFile file('{name}.keystore')
            storePassword providers.gradleProperty('{env}_STORE_PASSWORD').orNull
                ?: System.getenv('{env}_STORE_PASSWORD')
            keyAlias providers.gradleProperty('{env}_KEY_ALIAS').orNull
                ?: System.getenv('{env}_KEY_ALIAS')
            keyPassword providers.gradleProperty('{env}_KEY_PASSWORD').orNull
                ?: System.getenv('{env}_KEY_PASSWORD')
        }}""")
    return "\n".join(blocks)


def render() -> str:
    cfg = tomllib.loads(TOML.read_text())
    a = cfg["android"]
    deps = cfg.get("dependencies", {})
    lines = []
    for config in ("implementation", "api", "compileOnly", "runtimeOnly", "testImplementation", "androidTestImplementation"):
        for item in deps.get(config, []):
            lines.append(dependency(item, config))
    types = a.get("buildTypes", [])
    extra_signing = release_signing_configs(types)
    return f"""// GENERATED by tools/module_toml_to_gradle.py from app/module.toml. Edit the toml, not this file.
plugins {{
    id 'com.android.application' version '8.2.2'
    id 'com.google.gms.google-services' version '4.4.1'
}}

android {{
    namespace '{a["namespace"]}'
    compileSdk {a["compileSdk"]}

    defaultConfig {{
        applicationId "{a["namespace"]}"
        minSdk {a["minSdk"]}
        targetSdk {a["targetSdk"]}
        versionCode {a["versionCode"]}
        versionName "{a["versionName"]}"
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }}

    signingConfigs {{
        // Fixed, non-secret DEBUG key so every debug build (local or CI) has the same signing
        // fingerprint. Register its SHA-1/SHA-256 in Firebase for Google sign-in.
        // Never use this for a release build.
        debug {{
            storeFile file('debug.keystore')
            storePassword 'android'
            keyAlias 'androiddebugkey'
            keyPassword 'android'
        }}
{extra_signing}
    }}

    buildTypes {{
{build_types(types)}
    }}

    compileOptions {{
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }}

    lint {{
        abortOnError true
    }}
}}

dependencies {{
{chr(10).join(lines)}
}}
"""


def main() -> int:
    text = render()
    if "--check" in sys.argv:
        if OUT.read_text() != text:
            print("app/build.gradle is not what app/module.toml produces. Run: python3 tools/module_toml_to_gradle.py")
            return 1
        return 0
    restore_root_files()
    OUT.write_text(text)
    print(f"wrote {OUT.relative_to(ROOT)} from {TOML.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
