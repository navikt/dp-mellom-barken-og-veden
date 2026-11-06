package no.nav.dagpenger.mellom.barken.og.veden.utbetaling

import io.kotest.matchers.equals.shouldBeEqual
import no.nav.dagpenger.mellom.barken.og.veden.objectMapper
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Status.UtbetalingStatus
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved.tilBase64
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDateTime
import java.util.UUID
import java.util.stream.Stream

class UtbetalingStatusHendelseTest {
    @ParameterizedTest(name = "{index} => status={0} forventer status={1}, event={2}")
    @MethodSource("statusProvider")
    fun `skal kunne sende utbetalingstatus for flere status`(
        status: Status,
        forventetStatusTekst: String,
        forventetEventNavn: String,
    ) {
        val sakId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001")
        val behandlingId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        val utbetalingStatusHendelse =
            UtbetalingStatusHendelse(
                ident = "12345678901",
                behandlingId = behandlingId,
                sakId = sakId,
                behandletHendelseId = "m1",
                behandletHendelseType = "type_m1",
                status = status,
            )

        val hendelse = objectMapper.readTree(utbetalingStatusHendelse.tilHendelse())
        hendelse["@event_name"].asString() shouldBeEqual forventetEventNavn
        hendelse["ident"].asString() shouldBeEqual "12345678901"
        hendelse["behandlingId"].asString() shouldBeEqual behandlingId.toString()
        hendelse["eksternBehandlingId"].asString() shouldBeEqual behandlingId.tilBase64()
        hendelse["sakId"].asString() shouldBeEqual sakId.toString()
        hendelse["eksternSakId"].asString() shouldBeEqual sakId.tilBase64()
        hendelse["meldekortId"].asString() shouldBeEqual "m1"
        hendelse["behandletHendelseId"].asString() shouldBeEqual "m1"
        hendelse["behandletHendelseType"].asString() shouldBeEqual "type_m1"
        hendelse["status"].asString() shouldBeEqual forventetStatusTekst
    }

    companion object {
        private val fixedTime: LocalDateTime = LocalDateTime.parse("2025-01-01T12:00:00")

        @JvmStatic
        fun statusProvider(): Stream<Arguments> =
            Stream.of(
                Arguments.of(Status.Mottatt(opprettet = fixedTime), "MOTTATT", "utbetaling_mottatt"),
                Arguments.of(Status.TilUtbetaling(UtbetalingStatus.SENDT), "TIL_UTBETALING", "utbetaling_sendt"),
                Arguments.of(Status.TilUtbetaling(UtbetalingStatus.MOTTATT), "TIL_UTBETALING", "utbetaling_sendt"),
                Arguments.of(Status.TilUtbetaling(UtbetalingStatus.FEILET), "TIL_UTBETALING", "utbetaling_feilet"),
                Arguments.of(Status.Ferdig(UtbetalingStatus.OK), "FERDIG", "utbetaling_utført"),
            )
    }
}
