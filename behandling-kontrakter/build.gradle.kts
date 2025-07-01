import de.undercouch.gradle.tasks.download.Download

plugins {
    id("common")
    id("ch.acanda.gradle.fabrikt") version "1.17.0"
    id("de.undercouch.download") version "5.6.0"
    `java-library`
}

tasks.named("runKtlintCheckOverMainSourceSet").configure {
    dependsOn("fabriktGenerate")
}

tasks.named("runKtlintFormatOverMainSourceSet").configure {
    dependsOn("fabriktGenerate")
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("src/main/kotlin", "${layout.buildDirectory.get()}/generated/src/main/kotlin"))
        }
    }
}
ktlint {
    filter {
        exclude { element -> element.file.path.contains("generated") }
    }
}

dependencies {
    implementation(libs.jackson.annotation)
}

tasks.register<Download>("hentOpenAPI") {
    src("https://raw.githubusercontent.com/navikt/dp-behandling/refs/heads/main/openapi/src/main/resources/behandling-api.yaml")
    dest(layout.buildDirectory.file("behandling-api.yaml"))
    overwrite(true)
    group = "openapi"
    description = "Henter OpenAPI spesifikasjonen fra github og lagrer den lokalt"
}

tasks {
    fabriktGenerate {
        dependsOn("hentOpenAPI")
    }
}

fabrikt {
    generate("behandling") {
        apiFile = file("$projectDir/build/behandling-api.yaml")
        basePackage = "no.nav.dagpenger.behandling.api"
        skip = false
        quarkusReflectionConfig = disabled
        typeOverrides {
            datetime = LocalDateTime
        }
        model {
            generate = enabled
            validationLibrary = NoValidation
            extensibleEnums = disabled
            sealedInterfacesForOneOf = enabled
            ignoreUnknownProperties = disabled
            nonNullMapValues = enabled
            serializationLibrary = Jackson
            suffix = "DTO"
        }
    }
}
