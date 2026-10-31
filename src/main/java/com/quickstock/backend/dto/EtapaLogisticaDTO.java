package com.quickstock.backend.dto;

import lombok.Getter;

@Getter
public class EtapaLogisticaDTO {

    private final String nome;
    private final boolean concluido;
    private final String horario;

    public EtapaLogisticaDTO(String nome, boolean concluido, String horario) {
        this.nome = nome;
        this.concluido = concluido;
        this.horario = horario;
    }
}
