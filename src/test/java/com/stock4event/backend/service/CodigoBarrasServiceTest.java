package com.stock4event.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodigoBarrasServiceTest {

    @Test
    void aceitaCodigosComDigitoVerificadorCorreto() {
        assertTrue(CodigoBarrasService.valido("7891000100103")); // EAN-13
        assertTrue(CodigoBarrasService.valido("96385074"));      // EAN-8
        assertTrue(CodigoBarrasService.valido("036000291452"));  // UPC-A
    }

    @Test
    void recusaDigitoErradoOuTamanhoInvalido() {
        assertFalse(CodigoBarrasService.valido("7891000100104"));
        assertFalse(CodigoBarrasService.valido("12345"));
        assertFalse(CodigoBarrasService.valido("78910001001A3"));
        assertFalse(CodigoBarrasService.valido(null));
    }

    @Test
    void codigoInternoEhEan13ValidoComPrefixo2() {
        String codigo = CodigoBarrasService.gerarInterno(7L, 1234L);
        assertEquals(13, codigo.length());
        assertTrue(codigo.startsWith("200070001234"));
        assertTrue(CodigoBarrasService.valido(codigo));
    }

    @Test
    void normalizaTirandoEspacosETracos() {
        assertEquals("7891000100103", CodigoBarrasService.normalizar(" 789-1000 100103 "));
        assertNull(CodigoBarrasService.normalizar("   "));
    }
}
