package com.v2ray.ang

/** Замена сгенерированного BuildConfig сборки Android. Версию ставит CI через свойство JVM. */
object BuildConfig {
    const val APPLICATION_ID = "com.ward.client"
    val VERSION_NAME: String = System.getProperty("ward.version")
        ?: BuildConfig::class.java.`package`?.implementationVersion
        ?: "dev"
    const val DEBUG = false
    const val BUILD_TYPE = "release"
    const val DISTRIBUTION = "Desktop"
    val VERSION_CODE: Int = VERSION_NAME.split('.', '-').take(3).mapNotNull { it.toIntOrNull() }
        .fold(0) { acc, v -> acc * 100 + v }
    val GIT_COMMIT: String = System.getProperty("ward.commit") ?: "unknown"
}
