plugins {
    id("gravasom.android.library")
    id("gravasom.android.hilt")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.gravasom.core.data"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:audio-engine"))
    implementation(libs.coroutines.android)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.tink.android)
    implementation(libs.paging.runtime)
    implementation(libs.timber)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.coroutines.test)
}
