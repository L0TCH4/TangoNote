plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "app.tango"
    compileSdk = 34
    defaultConfig {
        applicationId = "app.tango"; minSdk = 24; targetSdk = 34
        // GitHub Actionsの実行番号を使い、ビルドごとにバージョンを上げる(上書き更新に必要)
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "1.$versionCode"
    }
    signingConfigs {
        create("fixed") {
            storeFile = file("debug.keystore")
            storePassword = "android"; keyAlias = "androiddebugkey"; keyPassword = "android"
        }
    }
    buildTypes {
        getByName("debug") {
            // 毎回同じ署名にして、アンインストールなしで更新できるようにする
            if (file("debug.keystore").exists()) signingConfig = signingConfigs.getByName("fixed")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.webkit:webkit:1.11.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
}
