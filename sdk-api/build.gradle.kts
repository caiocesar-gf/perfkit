plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
}

val perfkitVersion = properties["PERFKIT_VERSION"] as String
val perfkitGroup = properties["PERFKIT_GROUP"] as String
val perfkitGithubUrl = properties["PERFKIT_GITHUB_URL"] as String

android {
    namespace = "com.perfkit.api"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    // Pure domain — only coroutines for Flow types in use case contracts
    api(libs.kotlinx.coroutines.core)
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = perfkitGroup
            artifactId = "sdk-api"
            version = perfkitVersion
            afterEvaluate { from(components["release"]) }

            pom {
                name.set("PerfKit :: sdk-api")
                description.set(
                    "Public API contracts for the PerfKit Android SDK — ViolationEvent, " +
                    "PerfKitConfig, use case interfaces. Safe to include in release builds as a no-op surface."
                )
                url.set(perfkitGithubUrl)
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("$perfkitGithubUrl/blob/main/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("caiocesar-gf")
                        name.set("Caio Cesar")
                        url.set("https://github.com/caiocesar-gf")
                    }
                }
                scm {
                    connection.set("scm:git:github.com/caiocesar-gf/perfkit.git")
                    developerConnection.set("scm:git:ssh://github.com/caiocesar-gf/perfkit.git")
                    url.set("$perfkitGithubUrl/tree/main")
                }
            }
        }
    }
}
