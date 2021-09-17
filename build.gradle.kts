plugins {
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.serialization")
    `maven-publish`
}

group = "it.pureorigins"
version = "1.0.1"

repositories {
    mavenCentral()
    maven(url = "https://nexus.velocitypowered.com/repository/maven-public/")
    maven(url = "https://jitpack.io")
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.0.0")
    compileOnly("com.github.PureOrigins:velocity-language-kotlin:3.0.0")
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
    implementation("org.freemarker:freemarker:2.3.31")
    implementation("org.slf4j:slf4j-nop:1.7.30")
    kapt("com.velocitypowered:velocity-api:3.0.0")
}

tasks {
    register<Jar>("fatJar") {
        duplicatesStrategy = DuplicatesStrategy.WARN
        archiveClassifier.set("fat")
        from(configurations.runtimeClasspath.get().filter { !it.name.startsWith("kotlin") && !it.name.startsWith("annotations") }.map { if (it.isDirectory) it else zipTree(it) })
        from(sourceSets.main.get().output)
    }
    
    build {
        dependsOn("fatJar")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.github.PureOrigins"
            artifactId = project.name
            version = version

            from(components["kotlin"])
            artifact(tasks["kotlinSourcesJar"])
        }
    }
}
