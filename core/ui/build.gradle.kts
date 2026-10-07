plugins {
    id("gravasom.android.library")
    id("gravasom.android.compose")
}

android {
    namespace = "com.gravasom.core.ui"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.material.icons.extended)
}
