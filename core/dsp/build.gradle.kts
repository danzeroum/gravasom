plugins {
    id("gravasom.android.library")
}

android {
    namespace = "com.gravasom.core.dsp"
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
    implementation(project(":core:audio-engine"))
}
