plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

val perfkitVersion = properties["PERFKIT_VERSION"] as String
val perfkitGroup = properties["PERFKIT_GROUP"] as String
val perfkitGithubUrl = properties["PERFKIT_GITHUB_URL"] as String

android {
    namespace = "com.perfkit.debugui"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
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
    implementation(project(":sdk-core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = perfkitGroup
            artifactId = "sdk-debug-ui"
            version = perfkitVersion
            afterEvaluate { from(components["release"]) }

            pom {
                name.set("PerfKit :: sdk-debug-ui")
                description.set(
                    "Debug UI for PerfKit — persistent notification badge with violation count " +
                    "and a Jetpack Compose ViolationPanelActivity for real-time in-app inspection."
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
