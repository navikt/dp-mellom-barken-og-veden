package no.nav.dagpenger.mellom.barken.og.veden

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import kotlin.let

fun JsonNode.asUUID(): UUID = this.asText().let { UUID.fromString(it) }
