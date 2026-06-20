plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":protocol"))
    implementation(project(":execpolicy"))
    implementation(project(":tools"))
    implementation(project(":agent"))
    implementation("org.springframework.boot:spring-boot-starter-web:3.4.7")
    implementation("org.springframework.boot:spring-boot-starter-websocket:3.4.7")
    implementation("org.springframework.boot:spring-boot-starter-actuator:3.4.7")
    implementation("io.micrometer:micrometer-registry-prometheus:1.14.1")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.4.7")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
