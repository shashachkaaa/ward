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
    implementation("io.github.kyant0:shapes:1.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.11.0")
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("com.squareup.okhttp3:okhttp:5.5.0")
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("sh.calvin.reorderable:reorderable:3.1.0")
    implementation("com.google.zxing:core:3.5.4")
    implementation("org.json:json:20250517")
    implementation(compose.materialIconsExtended)
    testImplementation(kotlin("test"))
}

// Ресурсы приложения под Android - строки, массивы и картинки - берутся прямо
// из его res/: так перевод и значки остаются общими, а не расходятся копиями.
// Задача строит класс R с теми же именами, что на Android, и кладёт исходные
// XML в ресурсы; разбирает их AndroidResources во время работы
val androidRes = rootDir.resolve("../V2rayNG/app/src/main/res")
val genRes = layout.buildDirectory.dir("generated/androidRes")

val generateAndroidR by tasks.registering {
    inputs.dir(androidRes)
    outputs.dir(genRes)
    doLast {
        val out = genRes.get().asFile
        out.deleteRecursively()
        val res = File(out, "resources/android-res")
        fun names(file: File, tags: Set<String>): List<String> {
            if (!file.exists()) return emptyList()
            val doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            val list = mutableListOf<String>()
            val nodes = doc.documentElement.childNodes
            for (i in 0 until nodes.length) {
                val n = nodes.item(i) as? org.w3c.dom.Element ?: continue
                if (n.tagName in tags) list += n.getAttribute("name")
            }
            return list
        }
        val values = androidRes.resolve("values")
        val strings = (names(values.resolve("strings.xml"), setOf("string")) +
            names(androidRes.resolve("values-ru/strings.xml"), setOf("string"))).distinct().sorted()
        val arrays = listOf("strings.xml", "arrays.xml").flatMap { names(values.resolve(it), setOf("string-array", "integer-array", "array")) }.distinct().sorted()
        val plurals = (names(values.resolve("strings.xml"), setOf("plurals")) +
            names(androidRes.resolve("values-ru/strings.xml"), setOf("plurals"))).distinct().sorted()
        val drawables = androidRes.resolve("drawable").listFiles()!!.map { it.nameWithoutExtension }.distinct().sorted()
        val mipmaps = androidRes.resolve("mipmap-xxxhdpi").listFiles()!!.map { it.nameWithoutExtension }.distinct().sorted()
        // Анимации переходов на компьютере не проигрываются, но код ссылается на их имена
        val anims = androidRes.resolve("anim").listFiles().orEmpty().map { it.nameWithoutExtension }.distinct().sorted()

        // Исходники для разбора во время работы
        listOf("values", "values-ru").forEach { dir ->
            androidRes.resolve(dir).listFiles()!!.filter { it.name == "strings.xml" || it.name == "arrays.xml" }
                .forEach { it.copyTo(File(res, "$dir/${it.name}")) }
        }
        // Загрузчик векторов Compose Desktop не знает ссылок на системные цвета -
        // подставляем их значения
        androidRes.resolve("drawable").listFiles()!!.forEach { f ->
            File(res, "drawable/${f.name}").apply { parentFile.mkdirs() }.writeText(
                f.readText()
                    .replace("@android:color/white", "#FFFFFFFF")
                    .replace("@android:color/black", "#FF000000")
            )
        }
        androidRes.resolve("mipmap-xxxhdpi").copyRecursively(File(res, "mipmap"))

        fun block(name: String, items: List<String>, base: Int) = buildString {
            append("    object $name {\n")
            items.forEachIndexed { i, n -> append("        const val $n = ${base + i}\n") }
            append("    }\n")
        }
        fun table(name: String, items: List<String>) =
            "    internal val ${name}Names = arrayOf(${items.joinToString { "\"$it\"" }})\n"
        val src = buildString {
            append("// Сгенерировано задачей generateAndroidR из res/ приложения под Android. Не править\n")
            append("package com.v2ray.ang\n\n@Suppress(\"ClassName\", \"unused\")\nobject R {\n")
            append(block("string", strings, 0x7f010000))
            append(block("array", arrays, 0x7f020000))
            append(block("plurals", plurals, 0x7f030000))
            append(block("drawable", drawables, 0x7f040000))
            append(block("mipmap", mipmaps, 0x7f050000))
            append(block("anim", anims, 0x7f060000))
            append(table("string", strings)); append(table("array", arrays)); append(table("plurals", plurals))
            append(table("drawable", drawables)); append(table("mipmap", mipmaps))
            append("}\n")
        }
        File(out, "kotlin/com/v2ray/ang/R.kt").apply { parentFile.mkdirs() }.writeText(src)
    }
}

kotlin.sourceSets["main"].kotlin.srcDir(genRes.map { it.dir("kotlin") })
sourceSets["main"].resources.srcDir(genRes.map { it.dir("resources") })
tasks.named("compileKotlin") { dependsOn(generateAndroidR) }
tasks.named("processResources") { dependsOn(generateAndroidR) }

compose.desktop {
    application {
        mainClass = "com.ward.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb)
            packageName = "Ward"
            // MSI требует версию вида X.Y.Z без суффиксов
            packageVersion = wardVersion.removePrefix("v").substringBefore('-')
            // Только латиница: WiX собирает MSI в кодировке 1252, и кириллица
            // в описании или имени производителя валит упаковку
            description = "Ward - Xray client"
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
    (findProperty("screens") as String?)?.let { systemProperty("ward.screens", it) }
    systemProperty("user.language", (findProperty("lang") as String?) ?: "ru")
}
