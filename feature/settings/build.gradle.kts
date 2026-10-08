plugins {
    id("gravasom.android.library")
    id("gravasom.android.compose")
    id("gravasom.android.hilt")
}

android {
    namespace = "com.gravasom.feature.settings"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.coroutines.android)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.timber)
}
