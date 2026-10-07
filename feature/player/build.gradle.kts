plugins {
    id("gravasom.android.library")
    id("gravasom.android.compose")
    id("gravasom.android.hilt")
}

android {
    namespace = "com.gravasom.feature.player"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.common)
    implementation(libs.coroutines.android)
    implementation(libs.timber)
}
