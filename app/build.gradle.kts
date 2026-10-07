plugins {
    id("gravasom.android.application")
    id("gravasom.android.compose")
    id("gravasom.android.hilt")
}

android {
    namespace = "com.gravasom"
    defaultConfig {
        applicationId = "com.gravasom"
        versionCode = 1
        versionName = "1.0.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:service"))
    implementation(project(":core:ui"))
    implementation(project(":feature:record"))
    implementation(project(":feature:library"))
    implementation(project(":feature:player"))
    implementation(project(":feature:settings"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.coroutines.android)
    implementation(libs.timber)
    implementation(libs.profileinstaller)
}
