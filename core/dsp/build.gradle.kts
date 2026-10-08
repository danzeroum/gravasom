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

    testImplementation(libs.junit4)
}

// O harness do VAD (ADR-014) roda o MESMO codigo Kotlin + JNI no host: libgravasom_dsp.so de host, compilada por
// scripts/build-dsp-host.sh. Os dois caminhos vem do ambiente; sem eles os testes de corpus sao ignorados.
tasks.withType<Test>().configureEach {
    System.getenv("GRAVASOM_DSP_HOST_LIB")?.let { systemProperty("java.library.path", it) }
    System.getenv("GRAVASOM_VAD_CORPUS")?.let { systemProperty("gravasom.vad.corpus", it) }
    System.getenv("GRAVASOM_VAD_REPORT")?.let { systemProperty("gravasom.vad.report", it) }
    // O corpus e as lib de host mudam fora do Gradle: nao reaproveitar resultado em cache.
    inputs.property("vadCorpus", System.getenv("GRAVASOM_VAD_CORPUS") ?: "")
    outputs.upToDateWhen { System.getenv("GRAVASOM_VAD_CORPUS") == null }
}
