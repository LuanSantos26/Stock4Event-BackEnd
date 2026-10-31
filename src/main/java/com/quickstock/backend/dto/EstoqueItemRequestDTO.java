package com.quickstock.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EstoqueItemRequestDTO {

    @NotNull
    private Long produtoId;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal quantidade;
}
