package com.ourinhosplay.app.data

/** Interruptor de equipamento de uma modalidade (tem bola, tem rede...). */
data class OptionDef(val key: String, val label: String, val short: String)

object OptionCatalog {
    private val bola = OptionDef("bola", "Tem bola", "Bola")
    private val colete = OptionDef("colete", "Tem colete", "Colete")
    private val rede = OptionDef("rede", "Tem rede", "Rede")
    private val raquete = OptionDef("raquete", "Tem raquete", "Raquete")
    private val bolinha = OptionDef("bolinha", "Tem bolinha", "Bolinha")
    private val agua = OptionDef("agua", "Tem água", "Água")

    fun forSport(s: Sport?): List<OptionDef> = when (s) {
        Sport.Futebol, Sport.Futsal -> listOf(bola, colete)
        Sport.Futevolei, Sport.Volei, Sport.Biribol -> listOf(bola, rede)
        Sport.Basquete -> listOf(bola)
        Sport.BeachTennis, Sport.Tenis, Sport.Padel, Sport.TenisDeMesa -> listOf(raquete, bolinha)
        Sport.Corrida, Sport.Ciclismo, Sport.Caminhada -> listOf(agua)
        Sport.Natacao, null -> emptyList()
    }

    /** Só as opções que fazem sentido para a modalidade, na ordem do catálogo. */
    fun resolve(s: Sport?, options: Map<String, Boolean>): List<Pair<OptionDef, Boolean>> =
        forSport(s).mapNotNull { d -> options[d.key]?.let { d to it } }
}
