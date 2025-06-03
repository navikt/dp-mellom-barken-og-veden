package no.nav.dagpenger.mellom.barken.og.veden.domene

data class Ident(
    val verdi: String,
) {
    init {
        require(verdi.matches(Regex("[0-9]{11}"))) { "Personident må ha 11 siffer" }
    }

    override fun toString(): String = "******IDENT******"
}
