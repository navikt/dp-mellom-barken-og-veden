package no.nav.dagpenger.mellom.barken.og.veden

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonMapperBuilder

val objectMapper: ObjectMapper =
    jacksonMapperBuilder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()
