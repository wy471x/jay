plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":protocol"))
    implementation(project(":agent"))
    implementation(project(":config"))
    implementation(project(":secrets"))
    implementation(project(":state"))
    implementation(project(":execpolicy"))
    implementation(project(":mcp"))
    implementation(project(":release"))
    implementation("info.picocli:picocli:4.7.6")
    implementation("org.springframework.boot:spring-boot-starter:3.4.7")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// ── Git SHA embedding (equivalent to build.rs) ───────────────────────

// Capture at configuration time to avoid project reference in doLast
val rootDir = rootProject.rootDir
val projectVersion = project.version.toString()
val buildDir = layout.buildDirectory

val generateBuildProperties = tasks.register("generateBuildProperties") {
    notCompatibleWithConfigurationCache("external process 'git' required for build SHA")
    doLast {
        var sha = "unknown"
        try {
            val proc = ProcessBuilder("git", "-C", rootDir.absolutePath,
                "rev-parse", "--short=12", "HEAD")
                .redirectErrorStream(true)
                .start()
            val output = proc.inputStream.bufferedReader().readText().trim()
            val exitCode = proc.waitFor()
            if (exitCode == 0 && output.isNotEmpty() && !output.startsWith("fatal:")) {
                sha = output
            }
        } catch (_: Exception) {}

        val outDir = buildDir.dir("generated-resources/build-info").get().asFile
        outDir.mkdirs()
        outDir.resolve("build.properties").writeText(
            "jay.version=${projectVersion}\njay.build-sha=${sha}\n"
        )
    }
}

sourceSets {
    main {
        resources {
            srcDir(layout.buildDirectory.dir("generated-resources/build-info"))
        }
    }
}

tasks.processResources {
    dependsOn(generateBuildProperties)
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
    manifest {
        attributes(
            "Implementation-Title" to "jay-cli",
            "Implementation-Version" to "${project.version}",
            "Implementation-Vendor" to "CodeWhale.net"
        )
    }
}

tasks.register<JavaExec>("jayShim") {
    group = "application"
    description = "Run the jay shim alias"
    mainClass.set("com.jay.cli.JayShim")
    classpath = sourceSets["main"].runtimeClasspath
}
