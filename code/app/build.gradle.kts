plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.impostersyndrom"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.impostersyndrom"
        minSdk = 24
        targetSdk = 35
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.auth)
    // Retrofit for HTTP requests
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    implementation(libs.swiperefreshlayout)
    implementation ("com.google.android.material:material:1.9.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation(libs.play.services.location)
    implementation(libs.ext.junit)

    implementation(libs.espresso.intents)
    implementation(libs.uiautomator)

    implementation(libs.play.services.maps)
    testImplementation("junit:junit:4.13.2")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("com.google.firebase:firebase-firestore:24.4.4")

    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")
    implementation("androidx.fragment:fragment:1.5.5")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("com.google.firebase:firebase-auth:23.2.0")
    testImplementation(libs.junit)
    implementation("com.google.android.material:material:1.9.0")
    implementation(platform("com.google.firebase:firebase-bom:33.8.0"))
    implementation("com.google.firebase:firebase-firestore:25.1.1")
    implementation("com.google.firebase:firebase-storage")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("com.google.android.material:material:1.9.0")// For TabLayout
    implementation("androidx.viewpager2:viewpager2:1.0.0") // For ViewPager2
    androidTestImplementation("androidx.test.espresso:espresso-idling-resource:3.5.1")
    testImplementation ("org.mockito:mockito-core:5.2.0")
    testImplementation("junit:junit:4.13.2")
    implementation("com.google.firebase:firebase-auth:22.3.1")
    implementation("com.google.android.gms:play-services-auth:20.7.0")

}