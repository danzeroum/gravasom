plugins {
    id("gravasom.android.library")
    id("gravasom.android.compose")
    id("gravasom.android.hilt")
}

android {
    namespace = "com.gravasom.feature.enhance"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:audio-engine"))
    implementation(project(":core:service"))
    implementation(project(":core:ui"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.coroutines.android)
    implementation(libs.androidx.compose.material.icons.extended)
}

dependencies {
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
