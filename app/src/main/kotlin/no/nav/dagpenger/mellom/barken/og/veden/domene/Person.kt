package no.nav.dagpenger.mellom.barken.og.veden.domene

@JvmInline
value class Person(
    val ident: String,
) {
    init {
        require(ident.matches(Regex("[0-9]{11}"))) { "Personident må ha 11 siffer" }
    }

    override fun toString(): String = "******IDENT******"
}
