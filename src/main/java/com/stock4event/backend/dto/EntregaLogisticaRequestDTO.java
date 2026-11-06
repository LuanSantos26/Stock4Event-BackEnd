package com.stock4event.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EntregaLogisticaRequestDTO {

    @Size(max = 120)
    private String nomeCliente;

    @Size(max = 80)
    private String cidade;
}
