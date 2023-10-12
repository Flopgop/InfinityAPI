plugins {
    id("java")
}

group = "net.flamgop"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url="https://maven.scijava.org/content/repositories/public/")
    maven(url="https://mvn.0110.be/releases")
}

dependencies {
    implementation("org.usb4java:usb4java:1.3.0")
    implementation("com.github.sealedtx:java-youtube-downloader:3.2.3")
    implementation("org.jetbrains:annotations:24.0.0")
    implementation("org.jline:jline:3.22.0")

    implementation("be.tarsos.dsp:core:2.5")
    implementation("be.tarsos.dsp:jvm:2.5")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("--enable-preview")
    options.encoding = "UTF-8"
}