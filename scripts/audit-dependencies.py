#!/usr/bin/env python3
"""Read declared versions and upstream stable metadata without changing the build."""
import concurrent.futures
import json
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
STABLE = re.compile(r"^\d+(?:\.\d+){1,3}$")
GOOGLE = "https://dl.google.com/dl/android/maven2/"
CENTRAL = "https://repo.maven.apache.org/maven2/"


def version_key(version):
    return tuple(int(part) for part in version.split("."))


def declared_dependencies():
    app = (ROOT / "app/build.gradle.kts").read_text()
    variables = dict(re.findall(r'val\s+(\w+)\s*=\s*"([^"]+)"', app))
    versions = {}
    for group, artifact, version in re.findall(r'"([\w.-]+):([\w.-]+):([^"\s]+)"', app):
        resolved = variables.get(version[1:]) if version.startswith("$") else version
        if resolved and STABLE.fullmatch(resolved):
            versions[(group, artifact)] = resolved
    build = (ROOT / "build.gradle.kts").read_text()
    plugins = dict(re.findall(r'id\("([^"]+)"\) version "([^"]+)"', build))
    for plugin, coordinate in [
        ("com.android.application", ("com.android.tools.build", "gradle")),
        ("org.jetbrains.kotlin.android", ("org.jetbrains.kotlin", "kotlin-gradle-plugin")),
        ("com.google.dagger.hilt.android", ("com.google.dagger", "hilt-android-gradle-plugin")),
    ]:
        if plugin in plugins:
            versions[coordinate] = plugins[plugin]
    return versions


def check_dependency(item):
    (group, artifact), current = item
    base = GOOGLE if group.startswith("androidx.") or group == "com.android.tools.build" else CENTRAL
    url = base + group.replace(".", "/") + "/" + artifact + "/maven-metadata.xml"
    row = {"dependency": group + ":" + artifact, "current": current, "source": url}
    try:
        with urllib.request.urlopen(url, timeout=15) as response:
            document = ET.fromstring(response.read(2 * 1024 * 1024))
        candidates = [v.text for v in document.findall("./versioning/versions/version") if v.text and STABLE.fullmatch(v.text)]
        if not candidates:
            raise ValueError("No stable versions")
        latest = max(candidates, key=version_key)
        row.update(latest_stable=latest, status="update_available" if version_key(latest) > version_key(current) else "current")
    except Exception as error:
        # No exception body, URL parameters, environment values or credentials enter the report.
        row.update(status="not_verified", error_type=type(error).__name__)
    return row


def check_toolchain():
    result = {"sdk_source": "https://dl.google.com/android/repository/repository2-3.xml",
              "gradle_source": "https://services.gradle.org/versions/current"}
    try:
        with urllib.request.urlopen(result["sdk_source"], timeout=15) as response:
            root = ET.fromstring(response.read(20 * 1024 * 1024))
        for element in root.iter():
            element.tag = element.tag.split("}")[-1]
        packages = []
        for package in root.findall("remotePackage"):
            channel = package.find("channelRef")
            if channel is not None and channel.get("ref") != "channel-0":
                continue
            revision = package.find("revision")
            if revision is None or int(revision.findtext("preview", "0")) != 0:
                continue
            version = ".".join(revision.findtext(part, "0") for part in ("major", "minor", "micro"))
            packages.append((package.get("path", ""), version))
        # Android minor API levels are published as android-37.0, android-37.1, etc.
        platforms = [match.group(1) for path, _ in packages
                     if (match := re.fullmatch(r"platforms;android-(\d+(?:\.\d+)?)", path))]
        result["latest_platform"] = max(platforms, key=version_key)
        result["build_tools"] = max((version for path, version in packages if path.startswith("build-tools;")), key=version_key)
        result["platform_tools"] = next(version for path, version in packages if path == "platform-tools")
    except Exception as error:
        result["sdk_not_verified"] = type(error).__name__
    try:
        with urllib.request.urlopen(result["gradle_source"], timeout=15) as response:
            result["gradle_stable"] = json.load(response)["version"]
    except Exception as error:
        result["gradle_not_verified"] = type(error).__name__
    return result


def main():
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as executor:
        rows = list(executor.map(check_dependency, sorted(declared_dependencies().items())))
    toolchain = check_toolchain()
    json.dump({"dependencies": rows, "toolchain": toolchain, "build_changed": False, "packages_installed": False}, sys.stdout, indent=2)
    print()
    return 1 if any(row["status"] == "not_verified" for row in rows) or any(key.endswith("not_verified") for key in toolchain) else 0


if __name__ == "__main__":
    raise SystemExit(main())
