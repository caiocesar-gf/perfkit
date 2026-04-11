plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
}

val perfkitVersion = properties["PERFKIT_VERSION"] as String
val perfkitGroup = properties["PERFKIT_GROUP"] as String
val perfkitGithubUrl = properties["PERFKIT_GITHUB_URL"] as String

android {
    namespace = "com.perfkit.strictmode"
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
    // Accesses PerfKit.violationSink and contracts via sdk-core (transitive: sdk-api)
    implementation(project(":sdk-core"))
    implementation(libs.androidx.core.ktx)
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = perfkitGroup
            artifactId = "sdk-strictmode"
            version = perfkitVersion
            afterEvaluate { from(components["release"]) }

            pom {
                name.set("PerfKit :: sdk-strictmode")
                description.set(
                    "StrictMode adapter for PerfKit — installs ThreadPolicy and VmPolicy with " +
                    "penaltyListener on API 28+ for full programmatic capture, with Logcat fallback on API 24–27."
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
