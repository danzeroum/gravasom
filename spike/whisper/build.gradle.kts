// Spike de transcricao (ADR-013): whisper.cpp + modelo EMBUTIDO no APK, sem tela e SEM permissao de internet.
// So entra no build com -Pspike (settings.gradle.kts); o app principal nao depende deste modulo.
//
//   spike/whisper/scripts/buscar-whisper.sh spike/whisper/third_party/whisper.cpp     (fonte por hash)
//   spike/whisper/scripts/buscar-modelos.sh  ~/.cache/gravasom-corpus/modelos          (modelos por hash)
//   ./gradlew -Pspike :spike:whisper:assembleRelease -PmodeloWhisper=$HOME/.cache/gravasom-corpus/modelos/ggml-tiny-q5_1.bin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val modelo: String? = providers.gradleProperty("modeloWhisper").orNull
val fonteWhisper: String = providers.gradleProperty("whisperSrc").orNull ?: "$projectDir/third_party/whisper.cpp"

android {
    namespace = "com.gravasom.spike.whisper"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.gravasom.spike.whisper"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "spike"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DWHISPER_SRC=$fonteWhisper", "-DANDROID_STL=c++_shared", "-DGGML_OPENMP=OFF", "-DGGML_LLAMAFILE=OFF",
                    "-DWHISPER_BUILD_EXAMPLES=OFF", "-DWHISPER_BUILD_TESTS=OFF", "-DWHISPER_BUILD_SERVER=OFF", "-DBUILD_SHARED_LIBS=OFF",
                )
            }
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    // O modelo vai no APK como asset SEM compressao: o tamanho medido e o do arquivo, e a leitura nao descomprime.
    androidResources { noCompress += "bin" }
    lint { checkReleaseBuilds = false }
    packaging { jniLibs { useLegacyPackaging = false } }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

// Copia o modelo escolhido para uma pasta de assets gerada (nada de modelo no git); sem -PmodeloWhisper o APK sai sem modelo.
val modeloAssets = layout.buildDirectory.dir("modelo-assets")
android.sourceSets.getByName("main").assets.srcDir(modeloAssets)
if (modelo != null) {
    val copiarModelo = tasks.register<Copy>("copiarModelo") {
        from(modelo)
        into(modeloAssets)
        rename { "modelo.bin" }
    }
    tasks.matching { it.name != "copiarModelo" && (it.name.contains("Assets") || it.name.contains("Lint")) }
        .configureEach { dependsOn(copiarModelo) }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core:1.6.1")
}
