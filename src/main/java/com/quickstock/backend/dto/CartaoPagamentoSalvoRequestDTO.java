package com.quickstock.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartaoPagamentoSalvoRequestDTO {

    @NotNull
    private Long empresaId;

    @NotBlank
    @Size(max = 20)
    private String tipo;

    @Size(max = 80)
    private String apelido;

    @NotBlank
    @Size(max = 40)
    private String bandeira;

    @NotBlank
    @Size(max = 4)
    private String ultimosDigitos;

    @NotBlank
    @Size(max = 5)
    private String validade;

    @NotBlank
    @Size(max = 120)
    private String titular;
}
