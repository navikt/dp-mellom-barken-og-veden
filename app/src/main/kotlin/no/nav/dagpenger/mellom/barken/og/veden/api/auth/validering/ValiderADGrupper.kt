package no.nav.dagpenger.mellom.barken.og.veden.api.auth.validering

import io.ktor.server.auth.jwt.JWTAuthenticationProvider
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.auth.jwt.JWTPrincipal
import no.nav.dagpenger.mellom.barken.og.veden.Configuration

internal fun JWTAuthenticationProvider.Config.autoriserADGrupper() {
    val saksbehandlerGruppe = Configuration.utvikler

    validate { jwtClaims ->
        jwtClaims.måInneholde(adGruppe = saksbehandlerGruppe)
        JWTPrincipal(jwtClaims.payload)
    }
}

private fun JWTCredential.måInneholde(adGruppe: String) =
    require(
        this.payload.claims["groups"]
            ?.asList(String::class.java)
            ?.contains(adGruppe) ?: false,
    ) { "Mangler tilgang" }
