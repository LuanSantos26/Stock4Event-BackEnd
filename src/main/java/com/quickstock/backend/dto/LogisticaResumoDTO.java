package com.quickstock.backend.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class LogisticaResumoDTO {

    private final boolean vazio;
    private final String status;
    private final String tempoEstimado;
    private final String pedido;
    private final String motorista;
    private final String rota;
    private final List<EtapaLogisticaDTO> etapas;

    private LogisticaResumoDTO(
            boolean vazio,
            String status,
            String tempoEstimado,
            String pedido,
            String motorista,
            String rota,
            List<EtapaLogisticaDTO> etapas
    ) {
        this.vazio = vazio;
        this.status = status;
        this.tempoEstimado = tempoEstimado;
        this.pedido = pedido;
        this.motorista = motorista;
        this.rota = rota;
        this.etapas = etapas;
    }

    public static LogisticaResumoDTO vazio() {
        return new LogisticaResumoDTO(true, null, null, null, null, null, List.of());
    }

    public static LogisticaResumoDTO de(
            String status,
            String tempoEstimado,
            String pedido,
            String motorista,
            String rota,
            List<EtapaLogisticaDTO> etapas
    ) {
        return new LogisticaResumoDTO(false, status, tempoEstimado, pedido, motorista, rota, etapas);
    }
}
