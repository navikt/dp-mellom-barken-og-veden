package no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Person
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Status
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Status.Ferdig
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Status.Mottatt
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Status.TilUtbetaling
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Status.UtbetalingStatus
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.UtbetalingVedtak
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Utbetalingsdag
import java.util.UUID
import javax.sql.DataSource

internal class UtbetalingPostgresRepository(
    private val dataSource: DataSource,
) : UtbetalingRepo {
    override fun hentAlleVedtakMedStatus(status: Status): List<UtbetalingVedtak> {
        sessionOf(dataSource).use { session ->
            return session.transaction { tx ->
                tx.run(
                    queryOf(
                        // language=PostgreSQL
                        """
                        select *
                        from utbetaling
                        where status = :status
                        order by opprettet
                        """.trimIndent(),
                        mapOf(
                            "status" to
                                when (status) {
                                    is Mottatt -> "MOTTATT"
                                    is TilUtbetaling -> "TIL_UTBETALING"
                                    is Ferdig -> "FERDIG"
                                },
                        ),
                    ).map { row ->
                        row.toUtbetalingVedtak(tx)
                    }.asList,
                )
            }
        }
    }

    override fun hentAlleMottatte(): List<UtbetalingVedtak> = hentAlleVedtakMedStatus(Mottatt)

    override fun harUtbetalingerSomVenterPåSvar(sakId: String): Boolean {
        sessionOf(dataSource).use { session ->
            return session
                .transaction { tx ->
                    tx.run(
                        queryOf(
                            // language=PostgreSQL
                            """
                            select *
                            from utbetaling
                            where status = :status
                              and sak_id = :sakId
                            order by opprettet asc
                            """.trimIndent(),
                            mapOf(
                                "status" to "TIL_UTBETALING",
                                "sakId" to sakId,
                            ),
                        ).map { row ->
                            row.toUtbetalingVedtak(tx)
                        }.asList,
                    )
                }.isNotEmpty()
        }
    }

    override fun hentAlleUtbetalingerForSak(sakId: String): List<UtbetalingVedtak> {
        sessionOf(dataSource).use { session ->
            return session.transaction { tx ->
                tx.run(
                    queryOf(
                        // language=PostgreSQL
                        """
                        select *
                        from utbetaling
                        where sak_id = :sakId
                        """.trimIndent(),
                        mapOf(
                            "sakId" to sakId,
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

    override fun oppdaterStatus(
        behandlingId: UUID,
        status: Status,
    ) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                oppdaterStatus(
                    behandlingId = behandlingId,
                    status = status,
                    tx = tx,
                )
            }
        }
    }

    override fun oppdaterStatus(
        behandlingId: UUID,
        status: Status,
        tx: TransactionalSession,
    ) {
        tx
            .run(
                queryOf(
                    // language=PostgreSQL
                    """
                    update utbetaling
                    set status = :status,
                        ekstern_status = :externStatus
                    where behandling_id = :behandlingId
                    """.trimIndent(),
                    mapOf(
                        "behandlingId" to behandlingId,
                        "status" to
                            when (status) {
                                is Mottatt -> "MOTTATT"
                                is TilUtbetaling -> "TIL_UTBETALING"
                                is Ferdig -> "FERDIG"
                            },
                        "externStatus" to
                            when (status) {
                                is Mottatt -> null
                                is TilUtbetaling -> status.eksternStatus.name
                                is Ferdig -> null
                            },
                    ),
                ).asUpdate,
            ).also {
                lagreStatus(behandlingId, status, tx)
                if (status is TilUtbetaling) {
                    lagreEksternStatus(behandlingId, status.eksternStatus, tx)
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
                                vedtakstidspunkt,
                                meldekort_id,
                                sak_id,
                                ident,
                                status,
                                saksbehandlet_av,
                                besluttet_av,
                                opprettet
                            ) values (
                                :behandlingId,
                                :basertPaaId,
                                :vedtakstidspunkt,
                                :meldekortId,
                                :sakId,
                                :ident,
                                :status,
                                :saksbehandletAv,
                                :besluttetAv,
                                :opprettet
                            )
                            """.trimIndent(),
                            mapOf(
                                "behandlingId" to vedtak.behandlingId,
                                "basertPaaId" to vedtak.basertPåBehandlingId,
                                "vedtakstidspunkt" to vedtak.vedtakstidspunkt,
                                "meldekortId" to vedtak.meldekortId,
                                "sakId" to vedtak.sakId,
                                "ident" to vedtak.ident.ident,
                                "status" to
                                    when (vedtak.status) {
                                        is Mottatt -> "MOTTATT"
                                        is TilUtbetaling -> "TIL_UTBETALING"
                                        is Ferdig -> "FERDIG"
                                    },
                                "externStatus" to
                                    when (vedtak.status) {
                                        is Mottatt -> null
                                        is TilUtbetaling -> (vedtak.status as TilUtbetaling).eksternStatus.name
                                        is Ferdig -> null
                                    },
                                "saksbehandletAv" to vedtak.saksbehandletAv,
                                "besluttetAv" to vedtak.besluttetAv,
                                "opprettet" to vedtak.opprettet,
                            ),
                        ).asUpdate,
                    ).also {
                        lagreStatus(vedtak.behandlingId, vedtak.status, tx)
                        if (vedtak.status is TilUtbetaling) {
                            lagreEksternStatus(
                                behandlingId = vedtak.behandlingId,
                                status = (vedtak.status as TilUtbetaling).eksternStatus,
                                tx = tx,
                            )
                        }
                        vedtak.utbetalinger.forEach { dag ->
                            lagreDag(vedtak.behandlingId, dag, tx)
                        }
                    }
            }
        }
    }

    private fun lagreStatus(
        behandlingId: UUID,
        status: Status,
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
                    "status" to
                        when (status) {
                            is Mottatt -> StatusDb.MOTTATT.name
                            is TilUtbetaling -> StatusDb.TIL_UTBETALING.name
                            is Ferdig -> StatusDb.FERDIG.name
                        },
                ),
            ).asUpdate,
        )
    }

    private fun lagreEksternStatus(
        behandlingId: UUID,
        status: UtbetalingStatus,
        tx: TransactionalSession,
    ) {
        tx.run(
            queryOf(
                // language=PostgreSQL
                """
                insert into ekstern_status (
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

    private fun Row.toUtbetalingVedtak(tx: TransactionalSession): UtbetalingVedtak {
        val behandlingId = UUID.fromString(string("behandling_id"))
        return UtbetalingVedtak(
            behandlingId = behandlingId,
            basertPåBehandlingId = uuidOrNull("basert_paa_id"),
            vedtakstidspunkt = localDateTime("vedtakstidspunkt"),
            meldekortId = string("meldekort_id"),
            sakId = string("sak_id"),
            ident = Person(string("ident")),
            besluttetAv = string("besluttet_av"),
            saksbehandletAv = string("saksbehandlet_av"),
            utbetalinger = hentDager(behandlingId, tx),
            status =
                when (StatusDb.valueOf(string("status"))) {
                    StatusDb.MOTTATT -> Mottatt
                    StatusDb.TIL_UTBETALING -> TilUtbetaling(UtbetalingStatus.valueOf(string("ekstern_status")))
                    StatusDb.FERDIG -> Ferdig
                },
            opprettet = localDateTime("opprettet"),
        )
    }

    private fun Row.toUtbetalingsdag(): Utbetalingsdag =
        Utbetalingsdag(
            meldeperiode = string("meldeperiode"),
            dato = localDate("dato"),
            sats = int("sats"),
            utbetaltBeløp = int("utbetalt_beløp"),
        )

    private enum class StatusDb {
        MOTTATT,
        TIL_UTBETALING,
        FERDIG,
    }
}
