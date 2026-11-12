package com.stock4event.backend.service;

import com.stock4event.backend.exception.CadastroException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DadosBancariosServiceTest {

    @Test
    void normalizaBancoAgenciaContaEDocumento() {
        assertEquals("001", DadosBancariosService.validarBanco("1"));
        assertEquals("237", DadosBancariosService.validarBanco("237"));
        assertEquals("1234-X", DadosBancariosService.validarAgencia("1234-x"));
        assertEquals("12345-6", DadosBancariosService.validarConta("12345-6"));
        assertEquals("123456", DadosBancariosService.validarConta("123456"));
        assertEquals("12345678000195", DadosBancariosService.validarDocumento("12.345.678/0001-95"));
    }

    @Test
    void recusaValoresInvalidos() {
        assertThrows(CadastroException.class, () -> DadosBancariosService.validarBanco("1234"));
        assertThrows(CadastroException.class, () -> DadosBancariosService.validarAgencia("12a4"));
        assertThrows(CadastroException.class, () -> DadosBancariosService.validarConta("abc"));
        assertThrows(CadastroException.class, () -> DadosBancariosService.validarDocumento("123"));
        assertThrows(CadastroException.class,
                () -> DadosBancariosService.validarOpcao("salario", DadosBancariosService.TIPOS_CONTA, "x"));
    }
}
