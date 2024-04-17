@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("java-library")
    alias(libs.plugins.org.jetbrains.kotlin.jvm)
}
kotlin {
    jvmToolchain(17)
}
dependencies{
    api(libs.bcprov.jdk15on)
    api(libs.bcpkix.jdk15on)
}