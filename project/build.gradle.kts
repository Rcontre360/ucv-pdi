plugins {
    kotlin("jvm") version "1.9.0"
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.andmal"
version = "0.0.2"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.openpnp:opencv:4.9.0-0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("org.pdi.MainKt")
}

javafx {
    version = "17.0.2"
    modules("javafx.controls", "javafx.fxml", "javafx.swing")
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
