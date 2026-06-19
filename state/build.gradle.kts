dependencies {
    implementation(project(":protocol"))
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc:3.4.7")
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")
    implementation("org.flywaydb:flyway-core:10.22.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
