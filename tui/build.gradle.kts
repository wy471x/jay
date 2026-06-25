dependencies {
    implementation(project(":core"))
    implementation(project(":protocol"))
    implementation(project(":agent"))
    implementation(project(":tools"))
    implementation(project(":state"))
    implementation(project(":hooks"))
    implementation(project(":config"))
    implementation("org.springframework.boot:spring-boot-starter:3.4.7")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    // TamboUI stack — per §3.12 of feasibility report
    // Using 0.3.0 releases from Maven Central (snapshots have TLS issues on this machine)
    implementation("dev.tamboui:tamboui-core:0.3.0")
    implementation("dev.tamboui:tamboui-tui:0.3.0")
    implementation("dev.tamboui:tamboui-widgets:0.3.0")
    implementation("dev.tamboui:tamboui-panama-backend:0.3.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
