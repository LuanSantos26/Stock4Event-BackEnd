package com.quickstock.backend.dto;

import lombok.Getter;

@Getter
public class MesPedidosDTO {

    private final String mes;
    private final String label;
    private final int quantidade;

    public MesPedidosDTO(String mes, String label, int quantidade) {
        this.mes = mes;
        this.label = label;
        this.quantidade = quantidade;
    }
}
