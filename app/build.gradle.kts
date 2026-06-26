plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    id("jacoco")
}

android {
    namespace = "com.neurotracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.neurotracker"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            enableAndroidTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.6.0")
    // Room database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.compose.animation.core.lint)
    implementation(libs.androidx.compose.ui.text)
    kapt(libs.androidx.room.compiler)


    // Test unitarios (JVM)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.10")
    androidTestImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // Test Room (instrumentados)
    androidTestImplementation("androidx.room:room-testing:2.6.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// ─── JaCoCo coverage report ──────────────────────────────────────────────────

jacoco {
    toolVersion = "0.8.12"
}

val jacocoExcludes = listOf(
    // Android boilerplate
    "**/R.class", "**/R\$*.class",
    "**/BuildConfig.*", "**/Manifest*.*",
    "**/*Test*.*", "android/**/*.*",
    // Kotlin/Compose generated code
    "**/*\$\$serializer*",
    "**/*ComposableSingletons*",
    "**/*\$\$inlined*",
    // Pure UI packages (no ViewModel logic, 0% coverage)
    "**/ui/theme/**",
    "**/ui/onboarding/**",
    "**/ui/welcome/**",
    "**/ui/tests/selector/**",
    "**/ui/tests/components/**",
    "**/ui/accessibility/**",
    // Entry points and navigation
    "**/MainActivity*",
    "**/Routes*",
    // Compose screen files (exclude Screen composables, keep ViewModels)
    "**/*Screen*",
    "**/*Screen\$*",
    // ViewModelFactory classes (thin boilerplate)
    "**/*ViewModelFactory*",
)

tasks.register<JacocoReport>("jacocoAndroidTestReport") {
    group = "Reporting"
    description = "Generates JaCoCo HTML/XML coverage report (data layer + ViewModels only)"

    reports {
        xml.required.set(true)
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/coverage/html"))
    }

    sourceDirectories.setFrom(files("${projectDir}/src/main/java"))

    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
            exclude(jacocoExcludes)
        },
        fileTree(layout.buildDirectory.dir("intermediates/javac/debug/classes")) {
            exclude(jacocoExcludes)
        }
    )

    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("outputs/code_coverage/debugAndroidTest/connected/**/*.ec")
        }
    )
}