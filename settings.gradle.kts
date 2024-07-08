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
        maven (url = "https://jitpack.io" )
    }
}

//rootProject.name = "NFCTag"
rootProject.name = "DigitalKeyCCCSample"
include(":app")

include(":EmulatorVehicleOem")
include(":DigitalKey")

include(":data:Iso7816")
include(":data:sharedmodel")
include(":data:core-data")

include(":tools:NfcTool")
include(":tools:Security")

