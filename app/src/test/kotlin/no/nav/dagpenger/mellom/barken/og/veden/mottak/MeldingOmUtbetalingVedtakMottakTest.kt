package no.nav.dagpenger.mellom.barken.og.veden.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import org.junit.jupiter.api.Test

class MeldingOmUtbetalingVedtakMottakTest {
    private val rapid =
        TestRapid().apply {
            MeldingOmUtbetalingVedtakMottak(
                rapidsConnection = this,
                service = mockk(relaxed = true),
            )
        }

    @Test
    fun `mottar melding om utbetaling vedtak`() {
        val json = javaClass.getResource("/test-data/Vedtak_fattet_innvilget.json")!!.readText()

        rapid.sendTestMessage(json)

        println(json)
    }
}
