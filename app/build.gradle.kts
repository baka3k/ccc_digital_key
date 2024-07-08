@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    id("com.google.gms.google-services")

}
kotlin {
    jvmToolchain(17)
}
android {
    namespace = "com.example.nfctag"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.nfctag"
        minSdk = 28
        versionCode = 10
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {


    implementation(libs.preference)
    implementation(libs.firebase.database.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.androidx.fragment.ktx)

    // nfc dependencyTool
    implementation(project(mapOf("path" to ":tools:NfcTool")))
    implementation(project(mapOf("path" to ":tools:Security")))
    implementation(project(mapOf("path" to ":data:core-data")))
    implementation(project(mapOf("path" to ":data:sharedmodel")))
    implementation(project(mapOf("path" to ":DigitalKey")))
    implementation(project(mapOf("path" to ":EmulatorVehicleOem")))
//    implementation(project(mapOf("path" to ":NfcEmulator")))
    // just for testing ui - will be removed
    implementation("com.github.GoodieBag:Pinview:v1.4")
    // just for testing database - will be removed
    // Import the BoM for the Firebase platform
//    implementation(platform("com.google.firebase:firebase-bom:32.2.2"))
//    implementation("com.google.firebase:firebase-database-ktx")

}