package com.quickstock.backend.dto;

import lombok.Getter;

@Getter
public class StatusPedidoDTO {

    private final String codigo;
    private final String label;
    private final int ordem;
    private final boolean concluida;
    private final boolean ativa;

    public StatusPedidoDTO(String codigo, String label, int ordem, boolean concluida, boolean ativa) {
        this.codigo = codigo;
        this.label = label;
        this.ordem = ordem;
        this.concluida = concluida;
        this.ativa = ativa;
    }
}
