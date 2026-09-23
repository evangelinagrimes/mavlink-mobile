plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))    // JUnit 4 backend for kotlin.test
    testImplementation(libs.kotlinx.coroutines.test)
}
