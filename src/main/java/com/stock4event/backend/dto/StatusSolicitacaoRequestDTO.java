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

    /**
     * Próxima etapa. Entrega: em_separacao, abastecendo, em_rota.
     * Retirada: em_separacao, pronto_retirada. Ou cancelada.
     * "entregue" não vale aqui: o cliente conclui em PATCH /{id}/concluir.
     */
    @NotBlank
    private String status;
}
