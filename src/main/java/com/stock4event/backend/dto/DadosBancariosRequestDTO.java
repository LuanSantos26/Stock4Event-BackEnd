package com.stock4event.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Corpo de PUT /api/dados-bancarios. */
@Data
public class DadosBancariosRequestDTO {

    @NotNull(message = "Empresa não informada.")
    private Long empresaId;

    @NotBlank(message = "Informe o código do banco.")
    private String bancoCodigo;

    @NotBlank(message = "Informe o nome do banco.")
    @Size(max = 80, message = "Nome do banco muito longo.")
    private String bancoNome;

    @NotBlank(message = "Informe a agência.")
    private String agencia;

    @NotBlank(message = "Informe a conta.")
    private String conta;

    @NotBlank(message = "Informe o tipo de conta.")
    private String tipoConta;

    @NotBlank(message = "Informe o titular da conta.")
    @Size(max = 120, message = "Nome do titular muito longo.")
    private String titular;

    @NotBlank(message = "Informe o CPF ou CNPJ do titular.")
    private String documento;

    private String tipoChavePix;

    @Size(max = 120, message = "Chave Pix muito longa.")
    private String chavePix;
}
