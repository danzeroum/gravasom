plugins {
    id("gravasom.android.library")
    id("gravasom.android.compose")
    id("gravasom.android.hilt")
}

android {
    namespace = "com.gravasom.core.service"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:audio-engine"))
    implementation(project(":core:audio-android"))
    implementation(project(":core:data"))
    implementation(project(":core:dsp"))
    implementation(libs.coroutines.android)
    implementation(libs.timber)
    implementation(libs.androidx.core.ktx)
    implementation(libs.glance.appwidget)
}
