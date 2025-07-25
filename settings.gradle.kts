plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    versionCatalogs {
        create("libs") {
            from("no.nav.dagpenger:dp-version-catalog:20250721.193.2633c7")
        }
    }
}

rootProject.name = "dp-mellom-barken-og-veden"

include("app")
include("utbetaling-api")
include("helved-kontrakter")
include("behandling-kontrakter")
