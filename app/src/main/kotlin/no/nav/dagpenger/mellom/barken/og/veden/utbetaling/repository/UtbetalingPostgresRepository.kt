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

class UtbetalingPostgresRepository(
    private val dataSource: DataSource,
) : UtbetalingRepo {
    override fun hentAlleVedtakMedStatus(status: Status.Type): List<UtbetalingVedtak> {
        sessionOf(dataSource).use { session ->
            return session.transaction { tx ->
                tx.run(
                    queryOf(
                        // language=PostgreSQL
                        """
                        SELECT *
                        FROM utbetaling
                        WHERE status = :status
                        ORDER BY opprettet
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

    override fun hentAlleIkkeFerdige(): List<UtbetalingVedtak> {
        sessionOf(dataSource).use { session ->
            return session.transaction { tx ->
                tx.run(
                    queryOf(
                        // language=PostgreSQL
                        """
                        SELECT *
                        FROM utbetaling
                        WHERE status != :status
                        ORDER BY behandling_id
                        """.trimIndent(),
                        mapOf(
                            "status" to "FERDIG",
                        ),
                    ).map { row ->
                        row.toUtbetalingVedtak(tx)
                    }.asList,
                )
            }
        }
    }

    override fun hentAlleMottatte(): List<UtbetalingVedtak> = hentAlleVedtakMedStatus(Status.Type.MOTTATT)

    override fun harUtbetalingerSomVenterPåSvar(sakId: UUID): Boolean {
        sessionOf(dataSource).use { session ->
            return session
                .transaction { tx ->
                    tx.run(
                        queryOf(
                            // language=PostgreSQL
                            """
                            SELECT *
                            FROM utbetaling
                            WHERE status = :status
                              AND sak_id = :sakId
                            ORDER BY behandling_id
                            """.trimIndent(),
                            mapOf(
                                "status" to Status.Type.TIL_UTBETALING.name,
                                "sakId" to sakId,
                            ),
                        ).map { row ->
                            row.toUtbetalingVedtak(tx)
                        }.asList,
                    )
                }.isNotEmpty()
        }
    }

    override fun hentAlleUtbetalingerForSak(sakId: UUID): List<UtbetalingVedtak> {
        sessionOf(dataSource).use { session ->
            return session.transaction { tx ->
                tx.run(
                    queryOf(
                        // language=PostgreSQL
                        """
                        SELECT *
                        FROM utbetaling
                        WHERE sak_id = :sakId
                        ORDER BY behandling_id
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
                        SELECT *
                        FROM utbetaling
                        WHERE behandling_id = :behandlingId
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
                SELECT *
                FROM utbetalingsdag
                WHERE behandling_id = :behandlingId
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
                    UPDATE utbetaling
                    SET status = :status,
                        ekstern_status = :externStatus,
                        sist_endret_tilstand = NOW()
                    WHERE behandling_id = :behandlingId
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
                            INSERT INTO utbetaling (
                                behandling_id,
                                basert_paa_id,
                                vedtakstidspunkt,
                                behandlet_hendelse_id,
                                sak_id,
                                ident,
                                status,
                                saksbehandlet_av,
                                besluttet_av,
                                opprettet
                            ) VALUES (
                                :behandlingId,
                                :basertPaaId,
                                :vedtakstidspunkt,
                                :behandletHendelseId,
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
                                "behandletHendelseId" to vedtak.behandletHendelseId,
                                "sakId" to vedtak.sakId,
                                "ident" to vedtak.person.ident,
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
                INSERT INTO status (
                    behandling_id,
                    status,
                    opprettet
                ) VALUES (
                    :behandlingId,
                    :status,
                    :opprettet
                )
                """.trimIndent(),
                mapOf(
                    "behandlingId" to behandlingId,
                    "status" to
                        when (status) {
                            is Mottatt -> Status.Type.MOTTATT.name
                            is TilUtbetaling -> Status.Type.TIL_UTBETALING.name
                            is Ferdig -> Status.Type.FERDIG.name
                        },
                    "opprettet" to status.opprettet,
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
                INSERT INTO ekstern_status (
                    behandling_id,
                    status
                ) VALUES (
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
                INSERT INTO utbetalingsdag (
                    behandling_id,
                    meldeperiode,
                    dato,
                    sats,
                    utbetalt_beløp
                ) VALUES (
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
        val behandlingId = uuid("behandling_id")
        return UtbetalingVedtak(
            behandlingId = behandlingId,
            basertPåBehandlingId = uuidOrNull("basert_paa_id"),
            vedtakstidspunkt = localDateTime("vedtakstidspunkt"),
            behandletHendelseId = string("behandlet_hendelse_id"),
            sakId = uuid("sak_id"),
            person = Person(string("ident")),
            besluttetAv = string("besluttet_av"),
            saksbehandletAv = string("saksbehandlet_av"),
            utbetalinger = hentDager(behandlingId, tx),
            status =
                when (Status.Type.valueOf(string("status"))) {
                    Status.Type.MOTTATT ->
                        Mottatt(
                            opprettet = localDateTime("opprettet"),
                        )

                    Status.Type.TIL_UTBETALING ->
                        TilUtbetaling(
                            eksternStatus =
                                UtbetalingStatus.valueOf(
                                    string("ekstern_status"),
                                ),
                            opprettet = localDateTime("opprettet"),
                        )

                    Status.Type.FERDIG ->
                        Ferdig(
                            opprettet = localDateTime("opprettet"),
                            // Hvis ekstern_status er null, sett til OK (for bakoverkompatibilitet)
                            eksternStatus = stringOrNull("ekstern_status")?.let { UtbetalingStatus.valueOf(it) } ?: UtbetalingStatus.OK,
                        )
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
}
