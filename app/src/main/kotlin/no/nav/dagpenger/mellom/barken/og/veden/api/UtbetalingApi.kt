package no.nav.dagpenger.mellom.barken.og.veden.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.util.UUID

internal fun Application.utbetalingApi() {
    routing {
        // swaggerUI(path = "openapi", swaggerFile = "utbetaling-api.yaml")

        get("/") { call.respond(HttpStatusCode.OK) }

        authenticate("azureAd") {
            route("utbetaling/{behandlingId}") {
                get {
                    val behandlingId = behandlingId()
                    println(behandlingId)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

private fun RoutingContext.behandlingId() =
    (
        call.parameters["behandlingId"]?.let { UUID.fromString(it) }
            ?: throw BadRequestException("Mangler behandlingId")
    )
