plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.palantir.docker") version "0.35.0"
    id("com.palantir.docker-run") version "0.35.0"
}


configurations {
    create("integrationTestImplementation").apply {
        extendsFrom(configurations.testImplementation.get())
    }
}

group = "com.perapoch"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    // Dropwizard BOM
    implementation(platform("io.dropwizard:dropwizard-bom:4.0.1"))
    implementation("io.dropwizard:dropwizard-core")
    implementation("io.dropwizard:dropwizard-jdbi3")
    implementation("ru.vyarus:dropwizard-guicey:7.0.0")
    implementation("ru.vyarus.guicey:guicey-jdbi3:7.0.0")

    implementation("com.h2database:h2:1.4.193") // TODO: upgrade and use libs
    implementation(libs.caching)


    testImplementation("io.dropwizard:dropwizard-testing")
    testImplementation(testLibs.bundles.testImpl)
    testRuntimeOnly(testLibs.bundles.testRuntime)
}


testing {
    suites {
        register<JvmTestSuite>("integrationTest") {
            dependencies {
                implementation(project())
                configurations.testImplementation.get()
                configurations.implementation.get()
            }

            sources {
                java {
                    setSrcDirs(listOf("src/integrationTest/java"))
                }
            }
        }
    }
}

tasks.named("check") {
    dependsOn(testing.suites.named("integrationTest"))
}

tasks.jar {
    manifest.attributes["Main-Class"] = "com.perapoch.tasksapp.TaskApplication"
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))


tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
    gradleVersion = "8.2"
}

tasks.getByName("dockerPrepare").dependsOn(tasks.getByName("shadowJar"))

docker {
    name = "hub.docker.com/perapoch/${project.name}:".plus(version)
    setDockerfile(file("Dockerfile"))
    buildArgs(mapOf("APP_JAR" to "${project.name}-".plus(version).plus("-all.jar")))
    copySpec.from("build/libs").into("dockerized")
    copySpec.from("src/config").into("dockerized")
}

dockerRun {
    name = project.name
    image = "hub.docker.com/perapoch/${project.name}:".plus(version)
    ports("8080:8080", "8081:8081")
}


tasks.register("publishDockerImage") {
    doFirst {
        println("Publishing Docker Image...")
    }
    doLast {
        exec {
            commandLine("sh", "./script/publish-docker-image.sh", "hub.docker.com/perapoch/${project.name}:${version}")
        }
    }
}