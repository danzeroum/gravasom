// Spike de UX da transcricao (Fase 1 do plano): prototipos Compose das 3 telas pedidas
// + o dialogo de consentimento. Fora do build normal (settings.gradle.kts, gate -Pspike),
// igual ao spike/whisper. Nao tem logica de produto: e para revisar desenho em preview/device.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
}

android {
    namespace = "com.gravasom.spike.transcricao.ux"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.gravasom.spike.transcricao.ux"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "spike-ux"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint { checkReleaseBuilds = false }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.05.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
