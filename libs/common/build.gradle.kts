plugins {
    java
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")
    implementation("org.springframework:spring-web:6.1.1")
    implementation("org.springframework:spring-webmvc:6.1.1")
    implementation("jakarta.validation:jakarta.validation-api:3.0.2")
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")
}
