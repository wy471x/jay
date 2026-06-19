dependencies {
    implementation(project(":protocol"))
    implementation("org.springframework.boot:spring-boot-starter:3.4.7")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
