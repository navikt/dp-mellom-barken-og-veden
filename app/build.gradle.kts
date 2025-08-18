plugins {
    id("common")
    application
}

dependencies {
    implementation(project(":utbetaling-api"))
    implementation(project(":helved-kontrakter"))
    implementation(project(":behandling-kontrakter"))

    implementation(libs.rapids.and.rivers)
    implementation(libs.konfig)
    implementation(libs.kotlin.logging)
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.postgres)
    implementation("io.ktor:ktor-server-sse:${libs.versions.ktor.get()}")
    implementation("com.github.navikt.tbd-libs:naisful-app:2025.08.16-09.21-71db7cad")
    implementation("com.github.navikt.tbd-libs:kafka:2025.08.16-09.21-71db7cad")
    implementation("io.ktor:ktor-server-swagger:${libs.versions.ktor.get()}")

    testImplementation("io.ktor:ktor-server-test-host-jvm:${libs.versions.ktor.get()}")
    testImplementation(libs.mockk)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.json)
    testImplementation("com.github.navikt.tbd-libs:naisful-test-app:2025.08.16-09.21-71db7cad")
    testImplementation(libs.rapids.and.rivers.test)
    testImplementation(libs.bundles.postgres.test)
}

application {
    mainClass = "no.nav.dagpenger.mellom.barken.og.veden.AppKt"
}
