dependencies {
    implementation(project(":protocol"))
    implementation(project(":tools"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
