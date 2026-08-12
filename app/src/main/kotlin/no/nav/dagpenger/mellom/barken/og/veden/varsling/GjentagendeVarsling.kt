package no.nav.dagpenger.mellom.barken.og.veden.varsling

import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

/**
 * Vurderer om noe som har ventet på en tilstandsendring siden et gitt tidspunkt bør varsles om,
 * og eventuelt varsles om på nytt med jevne mellomrom helt til tilstanden endrer seg.
 *
 * Vurderingen er en ren funksjon av hvor lenge det er ventet - det trengs ingen persistert
 * telling av hvor mange ganger det er varslet fra før (se [Varslingsvurdering.antallVarslerSåLangt]).
 * Det gjør klassen trygg å bruke fra en jobb som kjører periodisk uten å måtte huske egen tilstand
 * mellom kjøringene.
 *
 * @param minsteVentetidFørVarsel Hvor lenge det må ha ventet før det aller første varselet sendes.
 * @param tidMellomGjentatteVarsler Hvor ofte varselet gjentas etter det første, så lenge ventingen fortsetter.
 * @param toleransevindu Bredden på vinduet rundt hvert varslingspunkt der [vurder] rapporterer at det skal
 *   varsles nå. Må være større enn eller lik intervallet kallende kode faktisk sjekker med (f.eks.
 *   hvor ofte en periodisk jobb kjører) - ellers kan et varslingspunkt bli hoppet over mellom to sjekker.
 * @param klokke Brukes til å bestemme "nå". Kan overstyres i tester for å unngå avhengighet til
 *   systemklokken.
 */
class GjentagendeVarsling(
    private val minsteVentetidFørVarsel: Duration,
    private val tidMellomGjentatteVarsler: Duration,
    private val toleransevindu: Duration,
    private val klokke: Clock = Clock.systemDefaultZone(),
) {
    /**
     * Vurderer om det skal varsles nå, basert på hvor lenge det er ventet siden [tidspunkt].
     */
    fun vurder(tidspunkt: LocalDateTime): Varslingsvurdering = vurder(Duration.between(tidspunkt, LocalDateTime.now(klokke)))

    /**
     * Vurderer om det skal varsles nå, basert på en allerede beregnet [ventetid].
     */
    fun vurder(ventetid: Duration): Varslingsvurdering {
        if (ventetid < minsteVentetidFørVarsel) {
            return Varslingsvurdering(skalVarsleNå = false, ventetid = ventetid, antallVarslerSåLangt = 0)
        }

        val tidSidenFørsteVarslingspunkt = ventetid.minus(minsteVentetidFørVarsel)
        val antallVarslingspunkterPassert = tidSidenFørsteVarslingspunkt.dividedBy(tidMellomGjentatteVarsler)
        val tidSidenNærmesteVarslingspunkt =
            tidSidenFørsteVarslingspunkt.minus(tidMellomGjentatteVarsler.multipliedBy(antallVarslingspunkterPassert))

        return Varslingsvurdering(
            skalVarsleNå = tidSidenNærmesteVarslingspunkt < toleransevindu,
            ventetid = ventetid,
            antallVarslerSåLangt = (antallVarslingspunkterPassert + 1).toInt(),
        )
    }
}

data class Varslingsvurdering(
    val skalVarsleNå: Boolean,
    val ventetid: Duration,
    val antallVarslerSåLangt: Int,
)
