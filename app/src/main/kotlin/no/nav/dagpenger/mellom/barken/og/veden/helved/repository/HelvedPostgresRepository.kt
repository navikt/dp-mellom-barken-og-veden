package no.nav.dagpenger.mellom.barken.og.veden.helved.repository

import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.dagpenger.mellom.barken.og.veden.helved.ApiError
import no.nav.dagpenger.mellom.barken.og.veden.helved.BehandlingId
import no.nav.dagpenger.mellom.barken.og.veden.helved.Detaljer
import no.nav.dagpenger.mellom.barken.og.veden.helved.StatusReply

internal class HelvedPostgresRepository : HelvedRepo {
    override fun lagreStatusSvar(
        behandlingId: BehandlingId,
        svar: StatusReply,
        tx: TransactionalSession,
    ) {
        lagreDetaljer(behandlingId, svar.detaljer, tx)
        lagreFeil(behandlingId, svar.error, tx)
    }

    private fun lagreFeil(
        behandlingId: BehandlingId,
        feil: ApiError?,
        tx: TransactionalSession,
    ) {
        if (feil == null) return

        tx.run(
            queryOf(
                // language=PostgreSQL
                """
                insert into feil (
                    behandling_id,
                    status,
                    doc
                ) values (
                    :behandlingId,
                    :status,
                    :doc
                )
                """.trimIndent(),
                mapOf(
                    "behandlingId" to behandlingId.uuid,
                    "status" to feil.statusCode,
                    "doc" to feil.doc,
                ),
            ).asUpdate,
        )
    }

    private fun lagreDetaljer(
        behandlingId: BehandlingId,
        detaljer: Detaljer?,
        tx: TransactionalSession,
    ) {
        if (detaljer == null) return

        detaljer.linjer.forEach { linje ->
            tx.run(
                queryOf(
                    // language=PostgreSQL
                    """
                    insert into detaljer_svar (
                        behandling_id,
                        fom,
                        tom,
                        beløp,
                        sats,
                        klassekode
                    ) values (
                        :behandlingId,
                        :fom,
                        :tom,
                        :belop,
                        :sats,
                        :klassekode
                    ) on conflict (behandling_id, fom, tom) do nothing
                    """.trimIndent(),
                    mapOf(
                        "behandlingId" to behandlingId.uuid,
                        "fom" to linje.fom,
                        "tom" to linje.tom,
                        "belop" to linje.beløp.toInt(),
                        "sats" to linje.vedtakssats?.toInt(),
                        "klassekode" to linje.klassekode,
                    ),
                ).asUpdate,
            )
        }
    }
}
