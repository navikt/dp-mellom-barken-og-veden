package no.nav.dagpenger.mellom.barken.og.veden.repository

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.mellom.barken.og.veden.domene.UtbetalingStatus
import no.nav.dagpenger.mellom.barken.og.veden.domene.UtbetalingVedtak
import no.nav.dagpenger.mellom.barken.og.veden.domene.Utbetalingsdag
import java.util.UUID
import javax.sql.DataSource

internal class UtbetalingPostgresRepository(
    private val dataSource: DataSource,
) : UtbetalingRepo {
    override fun hentAlleVedtakMedStatus(status: UtbetalingStatus): List<UtbetalingVedtak> {
        sessionOf(dataSource).use { session ->
            return session.transaction { tx ->
                tx.run(
                    queryOf(
                        // language=PostgreSQL
                        """
                        select *
                        from utbetaling
                        where status = :status
                        order by opprettet asc
                        """.trimIndent(),
                        mapOf(
                            "status" to status.name,
                        ),
                    ).map { row ->
                        row.toUtbetalingVedtak(tx)
                    }.asList,
                )
            }
        }
    }

    override fun hentVedtak(behandlingId: UUID): UtbetalingVedtak? {
        sessionOf(dataSource).use { session ->
            return session.transaction { tx ->
                tx.run(
                    queryOf(
                        // language=PostgreSQL
                        """
                        select *
                        from utbetaling
                        where behandling_id = :behandlingId
                        """.trimIndent(),
                        mapOf(
                            "behandlingId" to behandlingId,
                        ),
                    ).map { row ->
                        row.toUtbetalingVedtak(tx)
                    }.asSingle,
                )
            }
        }
    }

    private fun Row.toUtbetalingVedtak(tx: TransactionalSession): UtbetalingVedtak {
        val behandlingId = UUID.fromString(string("behandling_id"))
        return UtbetalingVedtak(
            behandlingId = behandlingId,
            basertPåBehandlingId = uuidOrNull("basert_paa_id"),
            meldekortId = string("meldekort_id"),
            ident = string("ident"),
            behandletAv = stringOrNull("behandlet_av"),
            status = UtbetalingStatus.valueOf(string("status")),
            opprettet = localDateTime("opprettet"),
            utbetalinger = hentDager(behandlingId, tx),
        )
    }

    private fun hentDager(
        behandlingId: UUID,
        tx: TransactionalSession,
    ): List<Utbetalingsdag> =
        tx.run(
            queryOf(
                // language=PostgreSQL
                """
                select *
                from utbetalingsdag
                where behandling_id = :behandlingId
                """.trimIndent(),
                mapOf("behandlingId" to behandlingId),
            ).map { row ->
                row.toUtbetalingsdag()
            }.asList,
        )

    private fun Row.toUtbetalingsdag(): Utbetalingsdag =
        Utbetalingsdag(
            meldeperiode = string("meldeperiode"),
            dato = localDate("dato"),
            sats = int("sats"),
            utbetaltBeløp = int("utbetalt_beløp"),
        )

    override fun oppdaterStatus(
        behandlingId: UUID,
        status: UtbetalingStatus,
    ) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx
                    .run(
                        queryOf(
                            // language=PostgreSQL
                            """
                            update utbetaling
                            set status = :status
                            where behandling_id = :behandlingId
                            """.trimIndent(),
                            mapOf(
                                "behandlingId" to behandlingId,
                                "status" to status.name,
                            ),
                        ).asUpdate,
                    ).also {
                        lagreStatus(behandlingId, status, tx)
                    }
            }
        }
    }

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
                                status,
                                behandlet_av,
                                opprettet
                            ) values (
                                :behandlingId,
                                :basertPaaId,
                                :meldekortId,
                                :ident,
                                :status,
                                :behandletAv,
                                :opprettet
                            )
                            """.trimIndent(),
                            mapOf(
                                "behandlingId" to vedtak.behandlingId,
                                "basertPaaId" to vedtak.basertPåBehandlingId,
                                "meldekortId" to vedtak.meldekortId,
                                "ident" to vedtak.ident,
                                "status" to vedtak.status.name,
                                "behandletAv" to vedtak.behandletAv,
                                "opprettet" to vedtak.opprettet,
                            ),
                        ).asUpdate,
                    ).also {
                        lagreStatus(vedtak.behandlingId, vedtak.status, tx)
                        vedtak.utbetalinger.forEach { dag ->
                            lagreDag(vedtak.behandlingId, dag, tx)
                        }
                    }
            }
        }
    }

    private fun lagreStatus(
        behandlingId: UUID,
        status: UtbetalingStatus,
        tx: TransactionalSession,
    ) {
        tx.run(
            queryOf(
                // language=PostgreSQL
                """
                insert into status (
                    behandling_id,
                    status
                ) values (
                    :behandlingId,
                    :status
                )
                """.trimIndent(),
                mapOf(
                    "behandlingId" to behandlingId,
                    "status" to status.name,
                ),
            ).asUpdate,
        )
    }

    private fun lagreDag(
        behandlingId: UUID,
        dag: Utbetalingsdag,
        tx: TransactionalSession,
    ) {
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
