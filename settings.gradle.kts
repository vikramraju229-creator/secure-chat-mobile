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
        maven { 
            url = uri("https://zetetic.net/sqlcipher/maven") 
            isAllowInsecureProtocol = true
        }
    }
}
rootProject.name = "SecureChat"
include(":app")
