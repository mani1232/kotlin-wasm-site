import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    kotlin("multiplatform") version "2.0.0-Beta1"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0-Beta1" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1"
    java
}

group = "cc.worldmandia"
version = "1.0-SNAPSHOT"

val ktorVersion = "2.3.6"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev/")
}

kotlin {
    jvm {
        withJava()
        compilations.all {
            compilerOptions.configure {
                jvmTarget.set(JvmTarget.JVM_21)
            }
        }
    }
    jvmToolchain(21)
    js(IR) {
        browser {
            distribution {
                outputDirectory = File("$projectDir/src/jvmMain/resources/static/static-js")
            }
            commonWebpackConfig {
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {

                    static = (static ?: mutableListOf()).apply {
                        add(project.rootDir.path)
                    }
                }
            }
        }
        binaries.executable()
    }
    applyDefaultHierarchyTemplate()

    sourceSets.all {
        languageSettings {
            languageVersion = "2.0"
        }
    }

    tasks.getByName("jvmProcessResources") {
        dependsOn("jsBrowserProductionExecutableDistributeResources", "jsBrowserProductionWebpack", "jsBrowserDistribution")
    }

    sourceSets {
        val commonMain by getting {

            dependencies {

            }
        }
        val jvmMain by getting {
            apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

            val logbackVersion = "1.4.11"
            val tcnativeVersion = "2.0.62.Final"

            val osName = System.getProperty("os.name").lowercase()
            val tcnative_classifier = when {
                osName.contains("win") -> "windows-x86_64"
                osName.contains("linux") -> "linux-x86_64"
                osName.contains("mac") -> "osx-x86_64"
                else -> null
            }

            tasks {
                shadowJar {
                    manifest {
                        attributes("Main-Class" to "MainKt")
                    }
                    archiveClassifier.set("all")
                }
                build {
                    dependsOn(shadowJar)
                }
            }

            dependencies {
                implementation("at.favre.lib:bcrypt:0.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.mongodb:mongodb-driver-kotlin-coroutine:4.11.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.6.0")
                implementation("ch.qos.logback:logback-classic:$logbackVersion")
                implementation("io.ktor:ktor-server-compression-jvm:$ktorVersion")
                implementation("io.ktor:ktor-server-netty:$ktorVersion")
                implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
                implementation("io.ktor:ktor-server-default-headers-jvm:$ktorVersion")
                implementation("io.ktor:ktor-server-sessions:$ktorVersion")
                implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
                implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
                implementation("io.ktor:ktor-network-tls-certificates:$ktorVersion")
                implementation("io.ktor:ktor-server-websockets-jvm:$ktorVersion")
                implementation("io.netty:netty-tcnative-boringssl-static:$tcnativeVersion:linux-x86_64")
                //if (tcnative_classifier != null) {
                //    implementation("io.netty:netty-tcnative-boringssl-static:$tcnativeVersion:$tcnative_classifier")
                //} else {
                //    implementation("io.netty:netty-tcnative-boringssl-static:$tcnativeVersion")
                //}
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("io.ktor:ktor-server-test-host:$ktorVersion")
                implementation(kotlin("test"))
            }
        }
        val jsMain by getting {

            dependencies {
                implementation(npm("memfs", "4.6.0"))
                implementation(npm("tslib", "2.6.2"))
                implementation(npm("quill-delta", "5.1.0"))
                implementation(npm("rxjs", "7.8.1"))
            }
        }
        //val wasmJsMain by getting {
        //    apply(plugin = "com.github.johnrengelman.shadow")
//
        //    dependencies {
        //        implementation(npm("memfs", "4.6.0"))
        //        implementation(npm("tslib", "2.6.2"))
        //        implementation(npm("quill-delta", "5.1.0"))
        //        implementation(npm("rxjs", "7.8.1"))
        //        implementation("org.jetbrains.kotlinx:kotlinx-serialization-core-wasm-js:1.6.1-wasm1")
        //        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-wasm-js:1.6.1-wasm1")
        //        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-wasm-js:1.7.2-wasm3")
        //        implementation("io.ktor:ktor-client-core-wasm-js:3.0.0-wasm1")
        //    }
        //}
    }
}