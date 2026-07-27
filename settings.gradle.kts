pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AIManager"
include(":app")
include(":core:model")
include(":core:network")
include(":core:common")
include(":domain:manager")
include(":domain:orchestration")
include(":domain:context")
include(":domain:media")
include(":domain:voice")
include(":data")
include(":feature:chat")
include(":feature:settings")
include(":feature:skills")
include(":feature:gems")
include(":feature:canvas")
include(":feature:compare")
include(":feature:analytics")
