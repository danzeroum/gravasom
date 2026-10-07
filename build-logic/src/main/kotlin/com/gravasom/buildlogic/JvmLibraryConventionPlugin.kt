package com.gravasom.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        pluginManager.apply("io.gitlab.arturbosch.detekt")

        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        tasks.withType(KotlinCompile::class.java).configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
                freeCompilerArgs.add("-Xjvm-default=all")
            }
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }

        dependencies {
            add("testImplementation", "org.junit.jupiter:junit-jupiter:5.11.3")
            add("testImplementation", "org.junit.jupiter:junit-jupiter-params:5.11.3")
            add("testRuntimeOnly", "org.junit.jupiter:junit-jupiter-engine:5.11.3")
            add("testImplementation", "io.mockk:mockk:1.13.13")
            add("testImplementation", "app.cash.turbine:turbine:1.2.0")
            add("testImplementation", "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
        }
    }
}
