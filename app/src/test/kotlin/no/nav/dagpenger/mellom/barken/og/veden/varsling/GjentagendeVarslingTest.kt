package no.nav.dagpenger.mellom.barken.og.veden.varsling

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset

class GjentagendeVarslingTest {
    private val nå = LocalDateTime.of(2026, 8, 11, 12, 0, 0)
    private val klokke = Clock.fixed(nå.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private fun varsling(
        minsteVentetidFørVarsel: Duration = Duration.ofHours(1),
        tidMellomGjentatteVarsler: Duration = Duration.ofHours(1),
        toleransevindu: Duration = Duration.ofMinutes(1),
    ) = GjentagendeVarsling(
        minsteVentetidFørVarsel = minsteVentetidFørVarsel,
        tidMellomGjentatteVarsler = tidMellomGjentatteVarsler,
        toleransevindu = toleransevindu,
        klokke = klokke,
    )

    @Test
    fun `varsler ikke om ventetiden er kortere enn terskelen`() {
        val vurdering = varsling().vurder(nå.minusMinutes(59))

        vurdering.skalVarsleNå shouldBe false
        vurdering.antallVarslerSåLangt shouldBe 0
    }

    @Test
    fun `varsler rett etter at terskelen er passert`() {
        val vurdering = varsling().vurder(nå.minusHours(1).minusSeconds(30))

        vurdering.skalVarsleNå shouldBe true
        vurdering.antallVarslerSåLangt shouldBe 1
    }

    @Test
    fun `varsler ikke igjen midt mellom to varslingspunkter`() {
        val vurdering = varsling().vurder(nå.minusHours(1).minusMinutes(30))

        vurdering.skalVarsleNå shouldBe false
        vurdering.antallVarslerSåLangt shouldBe 1
    }

    @Test
    fun `varsler på nytt når neste intervall er passert, og teller opp antall varsler`() {
        val vurdering = varsling().vurder(nå.minusHours(2).minusSeconds(30))

        vurdering.skalVarsleNå shouldBe true
        vurdering.antallVarslerSåLangt shouldBe 2
    }

    @Test
    fun `antallVarslerSåLangt øker for hvert intervall som er passert, uavhengig av om vi er innenfor vinduet`() {
        val vurdering = varsling().vurder(nå.minusHours(4).minusMinutes(30))

        vurdering.skalVarsleNå shouldBe false
        vurdering.antallVarslerSåLangt shouldBe 4
    }

    @Test
    fun `vurder med tidspunkt gir samme svar som vurder med tilsvarende ventetid`() {
        val tidspunkt = nå.minusHours(1).minusSeconds(30)

        val vedTidspunkt = varsling().vurder(tidspunkt)
        val vedVentetid = varsling().vurder(Duration.between(tidspunkt, LocalDateTime.now(klokke)))

        vedTidspunkt shouldBe vedVentetid
    }
}
