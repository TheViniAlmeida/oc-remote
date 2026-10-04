# Dependency and SDK audit — 2026-10-04

Read-only audit of declared versions against Google Maven, Maven Central, the Android SDK stable-channel feed, and Gradle release metadata. No build versions or installed packages were changed. Newer versions below are available releases, not a tested compatible replacement set.

## Toolchain

| Component | Project / build environment | Available stable release | Evidence |
| --- | --- | --- | --- |
| Android compile / target SDK | 34 / 34 | API 37.2 in the queried stable-channel feed | [Android SDK metadata](https://dl.google.com/android/repository/repository2-3.xml) |
| SDK Build Tools | 34.0.0 | 37.0.0 | [SDK metadata](https://dl.google.com/android/repository/repository2-3.xml) |
| SDK Platform Tools | Local ADB already installed | 37.0.1 | [Release notes](https://developer.android.com/tools/releases/platform-tools) |
| Gradle wrapper | 8.6 | 9.8.0 | [Gradle release metadata](https://services.gradle.org/versions/current) |
| Java | JDK 17 | Retain the JDK supported by the selected AGP/Gradle combination | [AGP compatibility](https://developer.android.com/build/releases/about-agp) |
| KSP | Not used: this project uses kapt | N/A | build.gradle.kts and app/build.gradle.kts |

## Declared libraries and plugins

Compose libraries without explicit versions inherit the Compose BOM. Kotlin Android, serialization and Compose compiler plugins share the Kotlin version. All explicit artifact declarations are listed below, including test dependencies.

| Artifact | Declared | Available stable | Metadata |
| --- | --- | --- | --- |
| `androidx.activity:activity-compose` | 1.9.1 | 1.13.0 | [Upstream](https://dl.google.com/dl/android/maven2/androidx/activity/activity-compose/maven-metadata.xml) |
| `androidx.appcompat:appcompat` | 1.7.0 | 1.8.0 | [Upstream](https://dl.google.com/dl/android/maven2/androidx/appcompat/appcompat/maven-metadata.xml) |
| `androidx.compose:compose-bom` | 2024.12.01 | 2026.09.00 | [Upstream](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/maven-metadata.xml) |
| `androidx.core:core-ktx` | 1.13.1 | 1.19.1 | [Upstream](https://dl.google.com/dl/android/maven2/androidx/core/core-ktx/maven-metadata.xml) |
| `androidx.datastore:datastore-preferences` | 1.1.1 | 1.2.1 | [Upstream](https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-preferences/maven-metadata.xml) |
| `androidx.hilt:hilt-navigation-compose` | 1.2.0 | 1.4.0 | [Upstream](https://dl.google.com/dl/android/maven2/androidx/hilt/hilt-navigation-compose/maven-metadata.xml) |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.8.4 | 2.11.0 | [Upstream](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime-ktx/maven-metadata.xml) |
| `androidx.navigation:navigation-compose` | 2.7.7 | 2.10.2 | [Upstream](https://dl.google.com/dl/android/maven2/androidx/navigation/navigation-compose/maven-metadata.xml) |
| `androidx.test.espresso:espresso-core` | 3.6.1 | 3.7.0 | [Upstream](https://dl.google.com/dl/android/maven2/androidx/test/espresso/espresso-core/maven-metadata.xml) |
| `androidx.test.ext:junit` | 1.2.1 | 1.3.0 | [Upstream](https://dl.google.com/dl/android/maven2/androidx/test/ext/junit/maven-metadata.xml) |
| `androidx.webkit:webkit` | 1.11.0 | 1.17.1 | [Upstream](https://dl.google.com/dl/android/maven2/androidx/webkit/webkit/maven-metadata.xml) |
| `androidx.work:work-runtime-ktx` | 2.9.1 | 2.12.0 | [Upstream](https://dl.google.com/dl/android/maven2/androidx/work/work-runtime-ktx/maven-metadata.xml) |
| `com.android.tools.build:gradle` | 8.4.0 | 9.4.1 | [Upstream](https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/maven-metadata.xml) |
| `com.composables:icons-lucide` | 1.1.0 | 1.1.0 | [Upstream](https://repo.maven.apache.org/maven2/com/composables/icons-lucide/maven-metadata.xml) |
| `com.google.accompanist:accompanist-swiperefresh` | 0.34.0 | 0.36.0 | [Upstream](https://repo.maven.apache.org/maven2/com/google/accompanist/accompanist-swiperefresh/maven-metadata.xml) |
| `com.google.code.gson:gson` | 2.11.0 | 2.14.0 | [Upstream](https://repo.maven.apache.org/maven2/com/google/code/gson/gson/maven-metadata.xml) |
| `com.google.dagger:hilt-android` | 2.51 | 2.60.1 | [Upstream](https://repo.maven.apache.org/maven2/com/google/dagger/hilt-android/maven-metadata.xml) |
| `com.google.dagger:hilt-android-compiler` | 2.51 | 2.60.1 | [Upstream](https://repo.maven.apache.org/maven2/com/google/dagger/hilt-android-compiler/maven-metadata.xml) |
| `com.google.dagger:hilt-android-gradle-plugin` | 2.51 | 2.60.1 | [Upstream](https://repo.maven.apache.org/maven2/com/google/dagger/hilt-android-gradle-plugin/maven-metadata.xml) |
| `com.mikepenz:multiplatform-markdown-renderer` | 0.28.0 | 0.45.0 | [Upstream](https://repo.maven.apache.org/maven2/com/mikepenz/multiplatform-markdown-renderer/maven-metadata.xml) |
| `com.mikepenz:multiplatform-markdown-renderer-code` | 0.28.0 | 0.45.0 | [Upstream](https://repo.maven.apache.org/maven2/com/mikepenz/multiplatform-markdown-renderer-code/maven-metadata.xml) |
| `com.mikepenz:multiplatform-markdown-renderer-coil2` | 0.28.0 | 0.45.0 | [Upstream](https://repo.maven.apache.org/maven2/com/mikepenz/multiplatform-markdown-renderer-coil2/maven-metadata.xml) |
| `com.mikepenz:multiplatform-markdown-renderer-m3` | 0.28.0 | 0.45.0 | [Upstream](https://repo.maven.apache.org/maven2/com/mikepenz/multiplatform-markdown-renderer-m3/maven-metadata.xml) |
| `io.coil-kt:coil-compose` | 2.6.0 | 2.7.0 | [Upstream](https://repo.maven.apache.org/maven2/io/coil-kt/coil-compose/maven-metadata.xml) |
| `io.ktor:ktor-client-auth` | 2.3.11 | 3.6.0 | [Upstream](https://repo.maven.apache.org/maven2/io/ktor/ktor-client-auth/maven-metadata.xml) |
| `io.ktor:ktor-client-content-negotiation` | 2.3.11 | 3.6.0 | [Upstream](https://repo.maven.apache.org/maven2/io/ktor/ktor-client-content-negotiation/maven-metadata.xml) |
| `io.ktor:ktor-client-core` | 2.3.11 | 3.6.0 | [Upstream](https://repo.maven.apache.org/maven2/io/ktor/ktor-client-core/maven-metadata.xml) |
| `io.ktor:ktor-client-logging` | 2.3.11 | 3.6.0 | [Upstream](https://repo.maven.apache.org/maven2/io/ktor/ktor-client-logging/maven-metadata.xml) |
| `io.ktor:ktor-client-okhttp` | 2.3.11 | 3.6.0 | [Upstream](https://repo.maven.apache.org/maven2/io/ktor/ktor-client-okhttp/maven-metadata.xml) |
| `io.ktor:ktor-client-websockets` | 2.3.11 | 3.6.0 | [Upstream](https://repo.maven.apache.org/maven2/io/ktor/ktor-client-websockets/maven-metadata.xml) |
| `io.ktor:ktor-serialization-kotlinx-json` | 2.3.11 | 3.6.0 | [Upstream](https://repo.maven.apache.org/maven2/io/ktor/ktor-serialization-kotlinx-json/maven-metadata.xml) |
| `junit:junit` | 4.13.2 | 4.13.2 | [Upstream](https://repo.maven.apache.org/maven2/junit/junit/maven-metadata.xml) |
| `org.jetbrains.kotlin:kotlin-gradle-plugin` | 2.0.21 | 2.4.20 | [Upstream](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-gradle-plugin/maven-metadata.xml) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.8.1 | 1.11.0 | [Upstream](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-android/maven-metadata.xml) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.8.1 | 1.11.0 | [Upstream](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-test/maven-metadata.xml) |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.7.1 | 1.11.0 | [Upstream](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-json/maven-metadata.xml) |
| `sh.calvin.reorderable:reorderable` | 3.1.0 | 3.1.0 | [Upstream](https://repo.maven.apache.org/maven2/sh/calvin/reorderable/reorderable/maven-metadata.xml) |

## Compatibility work before upgrading

- Upgrade AGP and the Gradle wrapper as a supported pair. AGP 9 introduces built-in Kotlin and DSL changes; the current Kotlin Android/kapt/Hilt setup needs migration rather than only editing version numbers. [AGP 9 migration](https://developer.android.com/build/migrate-to-built-in-kotlin).
- Keep Kotlin Android, serialization and Compose compiler versions aligned. Validate Kotlin metadata requirements of newer libraries. [Kotlin release process](https://kotlinlang.org/docs/releases.html).
- Update Compose through a stable BOM so its UI libraries are selected together; validate the existing Markdown renderer and Material components. [Compose BOM](https://developer.android.com/develop/ui/compose/bom).
- Treat Ktor 3 and Coil 3 as separate migrations with API/import changes; verify HTTP/SSE/WebSocket/authentication and attachment behavior. [Ktor migration](https://ktor.io/docs/migrating-3.html), [Coil upgrade](https://coil-kt.github.io/coil/upgrading_to_coil3/).
- Raising targetSdk changes Android runtime behavior. Validate notifications, foreground services, file picking and edge-to-edge on the target platform, alongside the V1/V2 regression suite. [Target SDK migration](https://developer.android.com/about/versions/16/migration).

## Verification

The audit checked 37 explicit artifacts; 34 have a newer stable version; 0 metadata lookups were unverified. Available releases were not installed or compatibility-tested. The feature build uses the existing pinned dependencies.

Reproduce the artifact inventory with `python3 scripts/audit-dependencies.py`. This command only prints public metadata as JSON. SDK and Gradle are also checked by the script; their sources are linked above.
