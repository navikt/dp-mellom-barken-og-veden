package no.nav.dagpenger.mellom.barken.og.veden.helved.repository

import kotliquery.TransactionalSession
import no.nav.dagpenger.mellom.barken.og.veden.helved.BehandlingId
import no.nav.dagpenger.mellom.barken.og.veden.helved.StatusReply

interface HelvedRepo {
    fun lagreStatusSvar(
        behandlingId: BehandlingId,
        svar: StatusReply,
        tx: TransactionalSession,
    )
}
