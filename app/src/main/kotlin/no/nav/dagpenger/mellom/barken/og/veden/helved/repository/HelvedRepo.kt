package no.nav.dagpenger.mellom.barken.og.veden.helved.repository

import kotliquery.TransactionalSession
import no.nav.dagpenger.mellom.barken.og.veden.helved.StatusReply
import java.util.UUID

interface HelvedRepo {
    fun lagreStatusSvar(
        behandlingId: UUID,
        svar: StatusReply,
        tx: TransactionalSession,
    )
}
