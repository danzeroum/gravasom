// Feature de transcricao offline (Onda 3). Motor: whisper.cpp (ADR-013/016).
// O modelo NAO vai no APK: e baixado uma vez pelo usuario e verificado por SHA-256.
// O fonte do whisper.cpp vem de -PwhisperSrc; sem ele o build do nativo falha (igual ao spike).
plugins {
    id("gravasom.android.library")
    id("gravasom.android.compose")
    id("gravasom.android.hilt")
    alias(libs.plugins.ksp)
}

val whisperSrc: String? = providers.gradleProperty("whisperSrc").orNull

android {
    namespace = "com.gravasom.feature.transcribe"
    defaultConfig {
        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DGGML_OPENMP=OFF",
                    "-DGGML_LLAMAFILE=OFF",
                    "-DWHISPER_BUILD_EXAMPLES=OFF",
                    "-DWHISPER_BUILD_TESTS=OFF",
                    "-DWHISPER_BUILD_SERVER=OFF",
                    "-DBUILD_SHARED_LIBS=OFF",
                )
                if (whisperSrc != null) arguments += "-DWHISPER_SRC=$whisperSrc"
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
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":core:audio-engine"))
    implementation(project(":core:audio-android"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.coroutines.android)
    implementation(libs.timber)
    implementation(libs.datastore.preferences)

    implementation(libs.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.coroutines.test)
    androidTestImplementation(libs.androidx.room.runtime)
    androidTestImplementation(libs.androidx.room.ktx)

    testImplementation(libs.junit4)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
}
