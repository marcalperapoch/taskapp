rootProject.name = "tasks-app"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("guice", "com.google.inject:guice:5.1.0")
            library("h2", "com.h2database:h2:2.1.214")
            library("caching", "com.github.ben-manes.caffeine:caffeine:3.1.2")
        }
        create("testLibs") {
            version("junit", "5.9.2")
            library("junit-jupiter-api", "org.junit.jupiter", "junit-jupiter-api").versionRef("junit")
            library("junit-jupiter-engine", "org.junit.jupiter", "junit-jupiter-engine").versionRef("junit")
            library("junit-jupiter-params", "org.junit.jupiter", "junit-jupiter-engine").versionRef("junit")
            library("assertj-core", "org.assertj:assertj-core:3.24.2")
            library("mockito", "org.mockito:mockito-junit-jupiter:5.3.1")
            bundle("testImpl", listOf("junit-jupiter-api", "assertj-core", "mockito", "junit-jupiter-params"))
            bundle("testRuntime", listOf("junit-jupiter-engine"))
        }
    }
}