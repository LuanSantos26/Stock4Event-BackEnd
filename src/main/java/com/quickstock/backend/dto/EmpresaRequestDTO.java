package com.quickstock.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmpresaRequestDTO {

    @NotBlank(message = "Nome da empresa é obrigatório.")
    @Size(max = 150)
    private String nome;

    @NotBlank(message = "CNPJ é obrigatório.")
    @Size(max = 18)
    private String cnpj;

    @Size(max = 20)
    private String telefone;
}
