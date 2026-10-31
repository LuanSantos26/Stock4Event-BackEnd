package com.quickstock.backend.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class MesValorDTO {

    private final String mes;
    private final String label;
    private final BigDecimal valor;

    public MesValorDTO(String mes, String label, BigDecimal valor) {
        this.mes = mes;
        this.label = label;
        this.valor = valor;
    }
}
