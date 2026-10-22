plugins { id("com.android.application") }

android {
  namespace = "com.screenpulse.healthshortcut"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.screenpulse.healthshortcut"
    minSdk = 26
    targetSdk = 35
    versionCode = 1
    versionName = "1.0.0"
  }

  signingConfigs {
    create("release") {
      storeFile = file(System.getenv("SCREENPULSE_KEYSTORE_PATH") ?: "key-not-provided.jks")
      storePassword = System.getenv("SCREENPULSE_STORE_PASSWORD") ?: ""
      keyAlias = "screenpulse"
      keyPassword = System.getenv("SCREENPULSE_STORE_PASSWORD") ?: ""
    }
  }
  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
      isShrinkResources = false
      signingConfig = signingConfigs.getByName("release")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}