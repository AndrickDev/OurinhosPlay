package com.ourinhosplay.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FacesTest {
    @Test fun inicial_maiuscula() = assertEquals("J", initialOf("joão"))

    @Test fun inicial_ignoraEspacos() = assertEquals("M", initialOf("  maria silva"))

    @Test fun inicial_vazioOuNulo() {
        assertEquals("?", initialOf(null))
        assertEquals("?", initialOf("   "))
    }

    @Test fun inicial_acentoMantem() = assertEquals("É", initialOf("érica"))

    @Test fun excedente_somaQuandoHaMaisQueMostrados() = assertEquals(3, hiddenFaces(taken = 8, shown = 5))

    @Test fun excedente_nuncaNegativo() = assertEquals(0, hiddenFaces(taken = 2, shown = 4))
}
