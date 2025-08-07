plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" // Añade esto
}

android {
    namespace = "com.example.cainflockers"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.cainflockers"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
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

    // --- ¡AÑADIDO SOLO ESTE BLOQUE PARA RESOLVER EL ERROR META-INF/INDEX.LIST! ---
    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/*.kotlin_module" // Para evitar otros posibles conflictos de Kotlin
            excludes += "META-INF/DEPENDENCIES" // <-- ¡AÑADE ESTA LÍNEA!**
        }
    }
    // --- FIN DEL BLOQUE A AÑADIR ---
}

dependencies {
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    // ... tus dependencias existentes
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.android.volley:volley:1.2.1")
    // Firebase BOM (Platform) para gestionar versiones de Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.2")) // <--- ¡AÑADE ESTA LÍNEA! (verifica la última versión en Firebase Docs)
    // Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging") // <--- ¡AÑADE ESTA LÍNEA!
    // Firebase Analytics (opcional, pero recomendado para monitoreo)
    implementation("com.google.firebase:firebase-analytics") // <--- ¡AÑADE ESTA LÍNEA!
    // Google API Client
    implementation("com.google.http-client:google-http-client-android:1.43.3")
    implementation("com.google.api-client:google-api-client-android:1.34.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.24.0")
    implementation("com.google.api-client:google-api-client-gson:1.33.2")
    implementation("com.google.apis:google-api-services-sheets:v4-rev20250616-2.0.0")// OAuth2 Sign-In
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.http-client:google-http-client-gson:1.44.2")
    // Kotlin CSV
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.7.0")
    implementation("io.coil-kt:coil-compose:2.6.0") // Puedes usar una versión más reciente si lo deseas
    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0") // Asegúrate de que esta línea esté ahí
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.activity:activity-ktx:1.7.2")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.volley)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}