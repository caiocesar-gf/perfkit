plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
}

android {
    namespace = "com.perfkit.api"
    compileSdk = 35

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
            groupId = "com.perfkit"
            artifactId = "sdk-api"
            version = "1.0.0"
            afterEvaluate { from(components["release"]) }
        }
    }
}
