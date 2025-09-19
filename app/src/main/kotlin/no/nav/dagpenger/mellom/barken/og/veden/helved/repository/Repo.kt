package no.nav.dagpenger.mellom.barken.og.veden.helved.repository

import kotliquery.sessionOf
import no.nav.dagpenger.mellom.barken.og.veden.helved.StatusReply
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Status
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.UtbetalingRepo
import java.util.UUID
import javax.sql.DataSource

internal class Repo(
    private val dataSource: DataSource,
    private val utbetalingRepo: UtbetalingRepo,
    private val helvedRepo: HelvedRepo,
) {
    fun lagreStatusFraHelved(
        behandlingId: UUID,
        status: Status,
        svar: StatusReply,
    ) {
        sessionOf(dataSource).use { session ->
            return session.transaction { tx ->
                utbetalingRepo.oppdaterStatus(behandlingId, status, tx)
                helvedRepo.lagreStatusSvar(behandlingId, svar, tx)
            }
        }
    }
}
