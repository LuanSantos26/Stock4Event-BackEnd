package com.stock4event.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusSolicitacaoRequestDTO {

    @NotNull
    private Long empresaFornecedoraId;

    /** em_rota, entregue ou cancelada. */
    @NotBlank
    private String status;
}
