import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.api.tasks.SourceSetContainer

plugins {
    id("org.springframework.boot") version "3.4.7" apply false
    id("jacoco")
    java
}

allprojects {
    group = "com.jay"
    version = "0.1.0"

    repositories {
        mavenCentral()
        maven {
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            mavenContent {
                snapshotsOnly()
            }
        }
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "checkstyle")
    apply(plugin = "jacoco")

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
        finalizedBy(tasks.jacocoTestReport)
    }

    tasks.withType<JacocoReport> {
        dependsOn(tasks.test)
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}

tasks.register<JacocoReport>("jacocoAggregateReport") {
    group = "verification"
    description = "Generates aggregated JaCoCo coverage report across all subprojects"

    val reportTasks = subprojects.mapNotNull { it.tasks.findByName("jacocoTestReport") }
    dependsOn(reportTasks)

    subprojects.forEach { sp ->
        sp.plugins.withType<JavaPlugin> {
            val mainSourceSet = sp.extensions.findByType<SourceSetContainer>()?.findByName("main")
            if (mainSourceSet != null) {
                additionalSourceDirs.from(mainSourceSet.allSource.srcDirs)
                classDirectories.from(mainSourceSet.output.classesDirs)
            }
            executionData.from(sp.layout.buildDirectory.file("jacoco/test.exec"))
        }
    }

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(true)
    }
}
