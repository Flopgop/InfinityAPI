plugins {
    id("java")
}

group = "net.flamgop"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.usb4java:usb4java:1.3.0")
    implementation("org.jetbrains:annotations:24.0.0")
    implementation("org.jline:jline:3.22.0")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("--enable-preview")
    options.encoding = "UTF-8"
}