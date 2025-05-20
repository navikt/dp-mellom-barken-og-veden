package no.nav.dagpenger.mellom.barken.og.veden.repository

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.mellom.barken.og.veden.domene.UtbetalingVedtak
import no.nav.dagpenger.mellom.barken.og.veden.domene.Utbetalingsdag
import java.util.UUID
import javax.sql.DataSource

internal class UtbetalingPostgresRepository(
    private val dataSource: DataSource,
) : UtbetalingRepo {
    override fun lagreVedtak(vedtak: UtbetalingVedtak) {
        sessionOf(dataSource).use {
            it.transaction { tx ->
                tx
                    .run(
                        queryOf(
                            // language=PostgreSQL
                            """
                            insert into utbetaling (
                                behandling_id,
                                basert_paa_id,
                                meldekort_id,
                                ident,
                                behandlet_av,
                                status,
                                opprettet
                            ) values (
                                :behandlingId,
                                :basertPaaId,
                                :meldekortId,
                                :ident,
                                :behandletAv,
                                :status,
                                :opprettet
                            )
                            """.trimIndent(),
                            mapOf(
                                "behandlingId" to vedtak.behandlingId,
                                "basertPaaId" to vedtak.basertPåBehandlingId,
                                "meldekortId" to vedtak.meldekortId,
                                "ident" to vedtak.ident,
                                "behandletAv" to vedtak.behandletAv,
                                "status" to vedtak.status.name,
                                "opprettet" to vedtak.opprettet,
                            ),
                        ).asUpdate,
                    ).also {
                        vedtak.utbetalinger.forEach { dag ->
                            lagreDag(vedtak.behandlingId, dag)
                        }
                    }
            }
        }
    }

    private fun lagreDag(
        behandlingId: UUID,
        dag: Utbetalingsdag,
    ) {
        sessionOf(dataSource).use {
            it.transaction { tx ->
                tx.run(
                    queryOf(
                        // language=PostgreSQL
                        """
                        insert into utbetalingsdag (
                            behandling_id,
                            meldeperiode,
                            dato,
                            sats,
                            utbetalt_beløp
                        ) values (
                            :behandlingId,
                            :meldeperiode,
                            :dato,
                            :sats,
                            :utbetaltBelop
                        )
                        """.trimIndent(),
                        mapOf(
                            "behandlingId" to behandlingId,
                            "meldeperiode" to dag.meldeperiode,
                            "dato" to dag.dato,
                            "sats" to dag.sats,
                            "utbetaltBelop" to dag.utbetaltBeløp,
                        ),
                    ).asUpdate,
                )
            }
        }
    }
}
