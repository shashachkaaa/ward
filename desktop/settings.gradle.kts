pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// Отдельный проект, а не модуль сборки Android: настольная версия не должна
// задевать ни её зависимости, ни её CI. Общий код сюда скопирован, см. README
rootProject.name = "ward-desktop"
