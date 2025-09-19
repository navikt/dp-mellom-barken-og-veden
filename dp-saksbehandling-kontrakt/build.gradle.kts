import de.undercouch.gradle.tasks.download.Download

plugins {
    id("common")
    id("ch.acanda.gradle.fabrikt") version "1.19.0"
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
    src("https://raw.githubusercontent.com/navikt/dp-saksbehandling/refs/heads/main/openapi/src/main/resources/saksbehandling-api.yaml")
    dest(layout.buildDirectory.file("saksbehandling-api.yaml"))
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
        apiFile = file("$projectDir/build/saksbehandling-api.yaml")
        basePackage = "no.nav.dagpenger.saksbehandling.api"
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
