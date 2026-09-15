package no.nav.dagpenger.mellom.barken.og.veden.utbetaling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.UUID

class SakIdMapperTest {
    private val sakIdMapper = SakIdMapper(resourcePath = "/test-data/test-sakid-mapping.tsv")

    @Test
    fun `bruker sakId fra mapping-tabellen når behandlingskjedeId finnes der`() {
        val behandlingskjedeId = UUID.fromString("019ad945-220d-7a1d-b5fa-fc9c2367827e")
        val forventetSakId = UUID.fromString("019ad945-2250-747c-9a9b-6f426f8cd199")

        sakIdMapper.sakIdFor(behandlingskjedeId) shouldBe forventetSakId
    }

    @Test
    fun `bruker behandlingskjedeId som sakId når den ikke finnes i mapping-tabellen`() {
        val behandlingskjedeId = UUID.randomUUID()

        sakIdMapper.sakIdFor(behandlingskjedeId) shouldBe behandlingskjedeId
    }

    @Test
    fun `feiler dersom mapping-fil ikke finnes`() {
        shouldThrow<IllegalStateException> { SakIdMapper(resourcePath = "/finnes-ikke.tsv") }
    }
}
