plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":protocol"))
    implementation("info.picocli:picocli")
    implementation("org.springframework.boot:spring-boot-starter:3.4.7")
}
