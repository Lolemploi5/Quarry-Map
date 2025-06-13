plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "1.9.10-1.0.13"
}

android {
    namespace = "com.example.quarrymap"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.quarrymap"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true // Activer Jetpack Compose
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3" // Alignez avec la version Kotlin utilisée
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
}

dependencies {
    // Jetpack Compose/UI
    implementation("androidx.compose.ui:ui:1.5.3")
    implementation("androidx.compose.material:material:1.5.3")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.3")
    implementation("androidx.activity:activity-compose:1.7.2")
    // Add Material3
    implementation("androidx.compose.material3:material3:1.1.2")

    // AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // Material Design
    implementation("com.google.android.material:material:1.9.0")

    //PhotoView pour les images
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    implementation("com.davemorrissey.labs:subsampling-scale-image-view:3.10.0")

    implementation("androidx.glance:glance-appwidget:1.0.0")

    // Glide pour les images
    implementation("com.github.bumptech.glide:glide:4.16.0")
    ksp("com.github.bumptech.glide:ksp:4.16.0")
    
    // Support SVG pour Glide
    implementation("com.caverock:androidsvg-aar:1.4")
    
    // Support TIFF avec Android-TiffBitmapFactory
    implementation("io.github.beyka:Android-TiffBitmapFactory:0.9.9.1")
    
    // Support PDF natif Android (PdfRenderer)
    // Pas de dépendance externe nécessaire - utilise l'API native Android

    // Google Play services location
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
