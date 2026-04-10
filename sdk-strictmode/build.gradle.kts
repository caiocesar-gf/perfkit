plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
}

android {
    namespace = "com.perfkit.strictmode"
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
    // Acessa PerfKit.violationSink e os contratos via sdk-core (transitivo: sdk-api)
    implementation(project(":sdk-core"))
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.perfkit"
            artifactId = "sdk-strictmode"
            version = "1.0.0"
            afterEvaluate { from(components["release"]) }
        }
    }
}
