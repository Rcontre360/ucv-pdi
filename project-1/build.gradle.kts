plugins {
    kotlin("jvm") version "2.2.21"
    application
}

group = "com.andmal"
version = "0.0.2"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("org.pdi.MainKt")
}

tasks.jar {
    manifest {
        attributes(mapOf("Main-Class" to "org.pdi.MainKt"))
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
//        configurations.runtimeClasspath.get().filter { it.isDirectory() ? it : zipTree(it) }

    })

}
