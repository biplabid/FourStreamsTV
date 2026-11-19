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

rootProject.name = "StreamCast"

include(":shared", ":mobile", ":tv")

project(":shared").projectDir = file("android-compose/shared")
project(":mobile").projectDir = file("android-compose/mobile")
project(":tv").projectDir = file("android-compose/tv")
