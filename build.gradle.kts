// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    `maven-publish`
}

// Aggregator POM — consumers add a single dependency:
//   debugImplementation("com.github.caiocesar-gf:perfkit:<version>")
// Gradle resolves all four SDK modules as transitive runtime deps.
val perfkitVersion = properties["PERFKIT_VERSION"] as String
val perfkitGithubUrl = properties["PERFKIT_GITHUB_URL"] as String
val perfkitModuleGroup = properties["PERFKIT_GROUP"] as String  // com.github.caiocesar-gf.perfkit

publishing {
    publications {
        create<MavenPublication>("perfkit") {
            groupId = "com.github.caiocesar-gf"
            artifactId = "perfkit"
            version = perfkitVersion
            pom {
                name.set("PerfKit")
                description.set("Real-time StrictMode violation detection and debug UI for Android.")
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
                withXml {
                    val deps = asNode().appendNode("dependencies")
                    listOf("sdk-api", "sdk-core", "sdk-strictmode", "sdk-debug-ui").forEach { module ->
                        val dep = deps.appendNode("dependency")
                        dep.appendNode("groupId", perfkitModuleGroup)
                        dep.appendNode("artifactId", module)
                        dep.appendNode("version", perfkitVersion)
                        dep.appendNode("scope", "runtime")
                    }
                }
            }
        }
    }
}