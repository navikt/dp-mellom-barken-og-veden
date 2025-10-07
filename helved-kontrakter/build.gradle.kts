plugins {
    id("ch.acanda.gradle.fabrikt") version "1.20.1"
    id("common")
    `java-library`
}

tasks {
    compileKotlin {
        dependsOn("fabriktGenerate")
    }
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

fabrikt {
    generate("helved-kontrakter") {
        apiFile = file("$projectDir/src/main/resources/helved-open-api.yaml")
        basePackage = "no.nav.helved.kontrakt.api"
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
