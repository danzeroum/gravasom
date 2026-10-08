plugins {
    id("gravasom.jvm.library")
    alias(libs.plugins.kover)
}

dependencies {
    implementation(libs.coroutines.core)
}

tasks.test {
    maxHeapSize = "96m"
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
