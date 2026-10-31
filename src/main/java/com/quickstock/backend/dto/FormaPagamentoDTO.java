package com.quickstock.backend.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class FormaPagamentoDTO {

    private final String metodo;
    private final String label;
    private final int percentual;
    private final BigDecimal valor;

    public FormaPagamentoDTO(String metodo, String label, int percentual, BigDecimal valor) {
        this.metodo = metodo;
        this.label = label;
        this.percentual = percentual;
        this.valor = valor;
    }
}
