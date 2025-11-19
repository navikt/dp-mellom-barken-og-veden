package no.nav.dagpenger.mellom.barken.og.veden.utbetaling.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Status
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.UtbetalingVedtak
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved.tilBase64
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.UtbetalingRepo
import no.nav.dagpenger.utbetaling.api.models.UtbetalingStatusDTO
import java.util.UUID

internal fun Application.utbetalingApi(repo: UtbetalingRepo) {
    routing {
        swaggerUI(path = "openapi", swaggerFile = "utbetaling-api.yaml")

        get("/") { call.respond(HttpStatusCode.OK) }

        authenticate("azureAd") {
            route("utbetaling") {
                get("{behandlingId}") {
                    val behandlingId = behandlingId()

                    call.respond(HttpStatusCode.OK)
                }

                get {
                    val sakId = call.queryParameters["sakId"]?.let { UUID.fromString(it) }

                    val utbetalinger =
                        if (sakId == null) {
                            repo.hentAlleIkkeFerdige().toUtbetalingStatusDTO()
                        } else {
                            repo.hentAlleUtbetalingerForSak(sakId).toUtbetalingStatusDTO()
                        }
                    call.respond(utbetalinger)
                }
            }
        }
    }
}

private fun List<UtbetalingVedtak>.toUtbetalingStatusDTO(): List<UtbetalingStatusDTO> =
    this.map { utbetaling ->
        UtbetalingStatusDTO(
            behandlingId = utbetaling.behandlingId,
            behandlingIdEkstern = utbetaling.behandlingId.tilBase64(),
            status = utbetaling.status::class.simpleName ?: "Ukjent",
            // dette er jo ikke nødvendigvis meldekortId lengre, men hendelseId.... Rename?
            meldekortId = utbetaling.hendelseId,
            sakId = utbetaling.sakId,
            sakIdEkstern = utbetaling.sakId.tilBase64(),
            ident = utbetaling.person.ident,
            opprettet = utbetaling.opprettet,
            vedtakstidspunkt = utbetaling.vedtakstidspunkt,
            eksternStatus = (utbetaling.status as? Status.TilUtbetaling)?.eksternStatus?.name,
        )
    }

private fun RoutingContext.behandlingId() =
    (
        call.parameters["behandlingId"]?.let { UUID.fromString(it) }
            ?: throw BadRequestException("Mangler behandlingId")
    )
