plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":protocol"))
    implementation(project(":agent"))
    implementation(project(":tools"))
    implementation(project(":execpolicy"))
    implementation(project(":config"))
    implementation(project(":state"))
    implementation("org.springframework.boot:spring-boot-starter:3.4.7")
}
