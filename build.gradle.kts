plugins {
    kotlin("jvm") version "1.4.10"
    idea
    application
}

group = "com.defiled"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("com.defiled.discov.DiscovKt")
}

repositories {
    mavenCentral()
}

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "1.8"

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("com.discord4j:discord4j-core:3.1.3")
}

tasks.withType<Jar> {
    manifest.attributes["Main-Class"] = application.mainClass
    configurations["compileClasspath"].forEach {
        from(zipTree(it.absoluteFile))
    }
}