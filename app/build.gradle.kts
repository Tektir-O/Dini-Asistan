plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// All future install-over-the-top Debug APKs must use the SAME signing key.
// The private keystore is provided only by environment variables at CI runtime,
// never included in the public repository or in app assets.
val stableKeyStoreFile = System.getenv("DINI_SIGNING_STORE_FILE")?.takeIf { it.isNotBlank() }
val stableStorePassword = System.getenv("DINI_SIGNING_STORE_PASSWORD")?.takeIf { it.isNotBlank() }
val stableKeyAlias = System.getenv("DINI_SIGNING_KEY_ALIAS")?.takeIf { it.isNotBlank() }
val stableKeyPassword = System.getenv("DINI_SIGNING_KEY_PASSWORD")?.takeIf { it.isNotBlank() }
val hasPersistentSigning = listOf(stableKeyStoreFile, stableStorePassword, stableKeyAlias, stableKeyPassword)
    .all { !it.isNullOrBlank() }

if (hasPersistentSigning) {
    require(java.io.File(stableKeyStoreFile!!).isFile) { "Persistent signing keystore missing." }
}

android {
    namespace = "com.dini.asistan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dini.asistan"
        minSdk = 26
        targetSdk = 34
        versionCode = (providers.gradleProperty("diniVersionCode").orNull ?: "4").toInt()
        versionName = providers.gradleProperty("diniVersionName").orNull ?: "0.4.0-premium-ayah"
    }

    signingConfigs {
        if (hasPersistentSigning) {
            create("persistentDebug") {
                storeFile = file(stableKeyStoreFile!!)
                storePassword = stableStorePassword
                keyAlias = stableKeyAlias
                keyPassword = stableKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            if (hasPersistentSigning) {
                signingConfig = signingConfigs.getByName("persistentDebug")
            }
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    androidResources {
        noCompress += listOf("opus", "ogg", "mp3", "m4a", "webp")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
