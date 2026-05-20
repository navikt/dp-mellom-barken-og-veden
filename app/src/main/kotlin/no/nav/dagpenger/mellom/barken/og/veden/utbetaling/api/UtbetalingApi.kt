package no.nav.dagpenger.mellom.barken.og.veden.utbetaling.api

import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.OutgoingMessage
import io.github.oshai.kotlinlogging.withLoggingContext
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
import io.ktor.server.util.getOrFail
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Status
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.UtbetalingStatusHendelse
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.UtbetalingVedtak
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.UtbetalingRepo
import no.nav.dagpenger.utbetaling.api.models.UtbetalingStatusDTO
import no.nav.dagpenger.utbetaling.api.models.UtbetalingsdagDTO
import java.time.LocalDate
import java.util.UUID

internal fun Application.utbetalingApi(
    repo: UtbetalingRepo,
    rapid: MessageContext,
) {
    routing {
        swaggerUI(path = "openapi", swaggerFile = "utbetaling-api.yaml")

        get("/") { call.respond(HttpStatusCode.OK) }

        authenticate("azureAd") {
            route("utbetaling") {
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
                get("dataprodukter") {
                    val fraOgMed = LocalDate.parse(call.queryParameters.getOrFail("fraOgMed"))
                    val dryRun = call.request.queryParameters["dryRun"]?.toBoolean() ?: false
                    val tilOgMed = call.queryParameters["tilOgMed"]?.let { LocalDate.parse(it) } ?: LocalDate.now()
                    val datalastId = UUID.randomUUID()
                    withLoggingContext(
                        "datalastId" to datalastId.toString(),
                        "fraOgMed" to fraOgMed.toString(),
                        "tilOgMed" to tilOgMed.toString(),
                    ) {
                        val utbetalinger = repo.hentAlleFerdige(fraOgMed, tilOgMed)
                        val meldinger =
                            utbetalinger.map {
                                OutgoingMessage(
                                    body =
                                        UtbetalingStatusHendelse(
                                            behandlingId = it.behandlingId,
                                            ident = it.person.ident,
                                            sakId = it.sakId,
                                            behandletHendelseId = it.behandletHendelseId,
                                            behandletHendelseType = it.behandletHendelseType,
                                            status = it.status,
                                        ).tilHendelse("_historisk"),
                                    key = it.person.ident,
                                )
                            }

                        val (_, notOk) = if (!dryRun) rapid.publish(meldinger) else Pair(emptyList(), emptyList())

                        call.respond(
                            DatalastkvitteringDTO(
                                datalastId = datalastId,
                                fraOgMed = fraOgMed,
                                tilOgMed = tilOgMed,
                                antallMeldinger = meldinger.size,
                                antallFeilsendinger = notOk.size,
                            ),
                        )
                    }
                }
            }
        }
    }
}

data class DatalastkvitteringDTO(
    val datalastId: UUID,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val antallMeldinger: Int,
    val antallFeilsendinger: Int,
)

private fun List<UtbetalingVedtak>.toUtbetalingStatusDTO(): List<UtbetalingStatusDTO> =
    this.map { utbetaling ->
        UtbetalingStatusDTO(
            behandlingId = utbetaling.behandlingId,
            behandlingIdEkstern = utbetaling.behandlingIdBase64,
            status = utbetaling.status::class.simpleName ?: "Ukjent",
            behandletHendelseId = utbetaling.behandletHendelseId,
            sakId = utbetaling.sakId,
            sakIdEkstern = utbetaling.sakIdBase64,
            ident = utbetaling.person.ident,
            opprettet = utbetaling.opprettet,
            vedtakstidspunkt = utbetaling.vedtakstidspunkt,
            eksternStatus =
                when (val status = utbetaling.status) {
                    is Status.Mottatt -> null
                    is Status.TilUtbetaling -> status.eksternStatus.name
                    is Status.Ferdig -> status.eksternStatus.name
                },
            utbetalingsdager =
                utbetaling.utbetalinger.map {
                    UtbetalingsdagDTO(
                        meldeperiodeId = it.meldeperiode,
                        dato = it.dato,
                        sats = it.sats,
                        utbetaltBeløp = it.utbetaltBeløp,
                    )
                },
        )
    }

private fun RoutingContext.behandlingId() =
    (
        call.parameters["behandlingId"]?.let { UUID.fromString(it) }
            ?: throw BadRequestException("Mangler behandlingId")
    )
