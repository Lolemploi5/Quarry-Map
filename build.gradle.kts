plugins {
    id("com.android.application") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.10" apply false
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}
