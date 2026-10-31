package com.quickstock.backend.dto;

import com.quickstock.backend.entity.Caminhoneiro;
import com.quickstock.backend.entity.EntregaLogistica;
import lombok.Getter;

import java.util.List;

@Getter
public class CaminhoneiroResponseDTO {

    private final Long id;
    private final String nome;
    private final String veiculo;
    private final String rota;
    private final String status;
    private final List<EntregaLogisticaResponseDTO> entregas;
    private final List<String> clientes;

    public CaminhoneiroResponseDTO(Caminhoneiro caminhoneiro) {
        this.id = caminhoneiro.getId();
        this.nome = caminhoneiro.getNome();
        this.veiculo = caminhoneiro.getVeiculo();
        this.rota = caminhoneiro.getRota();
        this.status = caminhoneiro.getStatus();
        List<EntregaLogistica> lista = caminhoneiro.getEntregas() == null
                ? List.of()
                : caminhoneiro.getEntregas();
        this.entregas = lista.stream().map(EntregaLogisticaResponseDTO::new).toList();
        this.clientes = lista.stream().map(EntregaLogistica::getNomeCliente).toList();
    }
}
