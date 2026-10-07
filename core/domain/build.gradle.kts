plugins {
    id("gravasom.jvm.library")
    alias(libs.plugins.kover)
}

dependencies {
    implementation(libs.coroutines.core)
}

kover {
    reports {
        verify {
            rule {
                minBound(80)
            }
        }
    }
}
