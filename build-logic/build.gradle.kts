plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("com.android.tools.build:gradle:8.7.3")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
    implementation("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.0.21-1.0.28")
}

gradlePlugin {
    plugins {
        register("gravasom.android.library") {
            id = "gravasom.android.library"
            implementationClass = "com.gravasom.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("gravasom.android.application") {
            id = "gravasom.android.application"
            implementationClass = "com.gravasom.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("gravasom.android.compose") {
            id = "gravasom.android.compose"
            implementationClass = "com.gravasom.buildlogic.AndroidComposeConventionPlugin"
        }
        register("gravasom.android.hilt") {
            id = "gravasom.android.hilt"
            implementationClass = "com.gravasom.buildlogic.AndroidHiltConventionPlugin"
        }
        register("gravasom.jvm.library") {
            id = "gravasom.jvm.library"
            implementationClass = "com.gravasom.buildlogic.JvmLibraryConventionPlugin"
        }
    }
}
