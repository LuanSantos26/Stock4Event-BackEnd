package com.quickstock.backend.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class MesLucroDTO {

    private final String mes;
    private final String label;
    private final BigDecimal lucro;
    private final BigDecimal gastos;

    public MesLucroDTO(String mes, String label, BigDecimal lucro, BigDecimal gastos) {
        this.mes = mes;
        this.label = label;
        this.lucro = lucro;
        this.gastos = gastos;
    }
}
