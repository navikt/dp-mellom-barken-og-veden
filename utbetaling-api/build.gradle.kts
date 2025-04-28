import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.exclude

plugins {
    id("common")
    `java-library`
    id("org.openapi.generator") version "7.13.0"
}

dependencies {
    implementation(libs.bundles.jackson)
}

sourceSets {
    main {
        kotlin {
            srcDir("build/generated/src/main/kotlin")
        }
    }
}

ktlint {
    filter {
        exclude("**")
    }
}

tasks {
    compileKotlin {
        dependsOn("openApiGenerate")
    }
    runKtlintFormatOverMainSourceSet {
        dependsOn("openApiGenerate")
    }
    runKtlintCheckOverMainSourceSet {
        dependsOn("openApiGenerate")
    }
}

openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set("$projectDir/src/main/resources/utbetaling-api.yaml")
    outputDir.set("${layout.buildDirectory.get()}/generated/")
    packageName.set("no.nav.dagpenger.utbetaling.api")
    globalProperties.set(mapOf("models" to ""))
    modelNameSuffix.set("DTO")
    configOptions.set(
        mapOf("serializationLibrary" to "jackson"),
    )
}
