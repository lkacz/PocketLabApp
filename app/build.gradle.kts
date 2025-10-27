plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.github.ben-manes.versions")
    jacoco
}

android {
    namespace = "com.lkacz.pola"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lkacz.pola"
        minSdk = 29
        targetSdk = 35
        versionCode = 119
        versionName = "1.1.9"

        buildConfigField("String", "APP_VERSION", "\"${'$'}versionName\"")
        buildConfigField("int", "APP_VERSION_CODE", versionCode.toString())

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    sourceSets {
        getByName("main") {
            assets.srcDirs(
                "src/main/assets",
                "../OnlineProtocolEditor/content",
            )
        }
    }
}

// JaCoCo configuration for unit test coverage
jacoco {
    toolVersion = "0.8.10"
}

tasks.withType<Test> {
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        // Exclude generated classes
        excludes = listOf("jdk.internal.*")
    }
}

val jacocoTestReport =
    tasks.register<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named("testDebugUnitTest"))
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
        val fileFilter =
            listOf(
                "**/R.class",
                "**/R$*.class",
                "**/BuildConfig.*",
                "**/Manifest*.*",
                "**/*Test*.*",
                "androidx/**",
                "**/databinding/**",
                "**/generated/**",
            )
        val javaClasses =
            fileTree("${'$'}{buildDir}/intermediates/javac/debug/classes") {
                exclude(fileFilter)
            }
        val kotlinClasses =
            fileTree("${'$'}{buildDir}/tmp/kotlin-classes/debug") {
                exclude(fileFilter)
            }
        classDirectories.setFrom(
            files(
                javaClasses,
                kotlinClasses,
            ),
        )
        sourceDirectories.setFrom(
            files(
                "src/main/java",
                "src/main/kotlin",
            ),
        )
        executionData.setFrom(
            files(
                "${'$'}{buildDir}/jacoco/testDebugUnitTest.exec",
            ),
        )
    }

dependencies {

    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    // Material Components (Views) for modern card/button styling in StartFragment
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.02"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("org.commonmark:commonmark:0.21.0")
    implementation("org.commonmark:commonmark-ext-gfm-tables:0.21.0")
}

ktlint {
    android.set(true)
    ignoreFailures.set(false)
    outputToConsole.set(true)
    verbose.set(true)
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**", "**/java/**")
    }
}
