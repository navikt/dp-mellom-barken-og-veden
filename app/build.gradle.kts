plugins {
    id("common")
    application
}

private val ktorVersion = libs.versions.ktor.get()

dependencies {
    implementation(project(":utbetaling-api"))
    implementation(project(":helved-kontrakter"))
    implementation(project(":dp-behandling-kontrakt"))
    implementation(project(":dp-saksbehandling-kontrakt"))

    implementation(libs.rapids.and.rivers)
    implementation(libs.konfig)
    implementation(libs.kotlin.logging)
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.ktor.client)
    implementation("no.nav.dagpenger:oauth2-klient:2025.08.20-08.53.9250ac7fbd99")
    implementation(libs.bundles.postgres)

    implementation("io.ktor:ktor-server-sse:$ktorVersion")
    implementation("com.github.navikt.tbd-libs:naisful-app:2025.09.15-16.10-ac41dc5c")
    implementation("com.github.navikt.tbd-libs:kafka:2025.09.15-16.10-ac41dc5c")
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")

    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation(libs.mockk)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.json)
    testImplementation("com.github.navikt.tbd-libs:naisful-test-app:2025.09.15-16.10-ac41dc5c")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation(libs.rapids.and.rivers.test)
    testImplementation(libs.bundles.postgres.test)
}

application {
    mainClass = "no.nav.dagpenger.mellom.barken.og.veden.AppKt"
}
