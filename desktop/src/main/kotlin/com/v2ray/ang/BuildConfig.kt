package com.v2ray.ang

/** Замена сгенерированного BuildConfig сборки Android. Версию ставит CI через свойство JVM. */
object BuildConfig {
    const val APPLICATION_ID = "com.ward.client"
    val VERSION_NAME: String = System.getProperty("ward.version")
        ?: BuildConfig::class.java.`package`?.implementationVersion
        ?: "dev"
    const val DEBUG = false
}
