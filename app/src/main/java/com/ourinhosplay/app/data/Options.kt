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

/** Quem assumiu levar um item ("Eu levo"). */
data class GearClaim(val item: String, val userId: String, val name: String)

/** Linha do painel de equipamento no detalhe do jogo. */
data class GearRow(val def: OptionDef, val on: Boolean?, val claim: GearClaim?, val mine: Boolean) {
    val text: String
        get() = when {
            claim != null -> def.short + " · " + (if (mine) "você leva" else claim.name.substringBefore(' ') + " leva")
            on == true -> def.label
            on == false -> "Sem " + def.short.lowercase()
            else -> def.short
        }
}

object GearRules {
    /**
     * Itens da modalidade com o estado de cada um. Quem não está na lista só vê o que o
     * criador marcou; quem está vê todos os itens da modalidade, para poder assumir algum.
     */
    fun rows(sport: Sport?, options: Map<String, Boolean>, claims: List<GearClaim>, me: String?, member: Boolean): List<GearRow> =
        OptionCatalog.forSport(sport)
            .filter { member || options.containsKey(it.key) }
            .map { d ->
                val c = claims.firstOrNull { it.item == d.key }
                GearRow(d, options[d.key], c, c != null && c.userId == me)
            }

    /** Pode tocar: livre (assumir) ou seu (soltar). Nunca o de outra pessoa. */
    fun canToggle(row: GearRow, member: Boolean, ended: Boolean): Boolean =
        member && !ended && (row.claim == null || row.mine)
}
