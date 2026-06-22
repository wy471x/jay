import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.plugins.quality.Checkstyle

plugins {
    id("org.springframework.boot") version "3.4.7" apply false
    java
}

allprojects {
    group = "com.jay"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "checkstyle")

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    extensions.configure<CheckstyleExtension> {
        toolVersion = "10.21.1"
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
        isIgnoreFailures = true
        maxWarnings = 0
    }

    tasks.withType<Checkstyle> {
        reports {
            html.required.set(true)
            xml.required.set(false)
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
