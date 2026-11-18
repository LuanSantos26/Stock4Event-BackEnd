package com.stock4event.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ItemSolicitacaoRequestDTO {

    @NotNull
    private Long produtoId;

    /** Em unidades, ou em número de paletes quando tipoVenda = "palete". */
    @NotNull
    @DecimalMin("0.001")
    private BigDecimal quantidade;

    /** "unidade" (padrão) ou "palete". */
    private String tipoVenda;
}
