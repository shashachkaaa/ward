import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.4.10"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
    id("org.jetbrains.compose") version "1.12.1"
}

// Версия берётся из свойства, чтобы CI ставил её из тега релиза
val wardVersion = (findProperty("wardVersion") as String?) ?: "1.0.0"

group = "com.ward"
version = wardVersion

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("io.github.kyant0:backdrop-desktop:2.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.11.0")
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("com.squareup.okhttp3:okhttp:5.5.0")
    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "com.ward.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb)
            packageName = "Ward"
            // MSI требует версию вида X.Y.Z без суффиксов
            packageVersion = wardVersion.removePrefix("v").substringBefore('-')
            description = "Ward - клиент Xray"
            vendor = "Ward"
            // Рядом с приложением кладётся бинарник xray и geo-файлы: CI скачивает
            // их в resources/<os> перед упаковкой
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
            modules("java.naming", "jdk.crypto.ec")
            windows {
                menuGroup = "Ward"
                shortcut = true
                dirChooser = true
                // Постоянный код обновления: без него новая версия ставится рядом со старой
                upgradeUuid = "6f0d8f5e-3b7a-4c55-9a1e-2f4d8b9c7e21"
                iconFile.set(project.file("icons/ward.ico"))
            }
            linux {
                shortcut = true
                iconFile.set(project.file("icons/ward.png"))
            }
        }
    }
}

// Снимок окна под виртуальным экраном: xvfb-run ./gradlew screenshot
tasks.register<JavaExec>("screenshot") {
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.ward.desktop.ScreenshotKt")
    args = listOf(
        (findProperty("shot") as String?) ?: layout.buildDirectory.file("screenshot.png").get().asFile.path,
        (findProperty("wait") as String?) ?: "6000",
        (findProperty("seed") as String?) ?: ""
    )
    (findProperty("dataDir") as String?)?.let { systemProperty("ward.dataDir", it) }
}
