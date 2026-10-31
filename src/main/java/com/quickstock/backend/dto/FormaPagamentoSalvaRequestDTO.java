package com.quickstock.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FormaPagamentoSalvaRequestDTO {

    @NotNull
    private Long empresaId;

    @NotBlank
    @Size(max = 20)
    private String tipo;

    @NotBlank
    @Size(max = 80)
    private String apelido;

    private boolean principal = false;
}
