package com.ourinhosplay.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RolesTest {
    private val roles = listOf(
        RoleStatus(role = "goleiro", slots = 2, taken = 1, perkPct = 100),
        RoleStatus(role = "levantador", slots = 2, taken = 0, perkPct = 50),
    )

    @Test fun payWeight_isento() = assertEquals(0.0, payWeight("goleiro", roles), 0.0)

    @Test fun payWeight_metade() = assertEquals(0.5, payWeight("levantador", roles), 0.0)

    @Test fun payWeight_semFuncaoPagaInteiro() {
        assertEquals(1.0, payWeight(null, roles), 0.0)
        assertEquals(1.0, payWeight("linha", roles), 0.0)
    }

    @Test fun open_nuncaNegativo() = assertEquals(0, RoleStatus("goleiro", slots = 1, taken = 3).open)

    @Test fun catalogo_porEsporte() {
        assertEquals("goleiro", RoleCatalog.forSport(Sport.Futebol)?.key)
        assertEquals("goleiro", RoleCatalog.forSport(Sport.Futsal)?.key)
        assertEquals("levantador", RoleCatalog.forSport(Sport.Volei)?.key)
        assertNotNull(RoleCatalog.forSport(Sport.Volei))
        assertNull(RoleCatalog.forSport(null))
    }

    @Test fun catalogo_rotuloDesconhecidoCapitaliza() = assertEquals("Ala", RoleCatalog.label("ala"))
}
