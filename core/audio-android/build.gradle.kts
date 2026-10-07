plugins {
    id("gravasom.android.library")
    id("gravasom.android.hilt")
}

android {
    namespace = "com.gravasom.core.audio.android"
    defaultConfig {
        externalNativeBuild {
            cmake {
                arguments += listOf("-DANDROID_STL=c++_shared")
            }
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:audio-engine"))
    implementation(libs.coroutines.android)
    implementation(libs.timber)
    implementation(libs.tracing.ktx)
}
