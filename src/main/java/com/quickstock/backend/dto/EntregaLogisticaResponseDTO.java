package com.quickstock.backend.dto;

import com.quickstock.backend.entity.EntregaLogistica;
import lombok.Getter;

@Getter
public class EntregaLogisticaResponseDTO {

    private final Long id;
    private final String nomeCliente;
    private final String cidade;
    private final String rota;
    private final String status;

    public EntregaLogisticaResponseDTO(EntregaLogistica entrega) {
        this.id = entrega.getId();
        this.nomeCliente = entrega.getNomeCliente();
        this.cidade = entrega.getCidade();
        this.rota = entrega.getRota();
        this.status = entrega.getStatus();
    }
}
