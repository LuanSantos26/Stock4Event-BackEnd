package com.quickstock.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CadastroContaRequestDTO {

    @NotNull(message = "Dados da empresa são obrigatórios.")
    @Valid
    private EmpresaRequestDTO empresa;

    @NotNull(message = "Dados do usuário são obrigatórios.")
    @Valid
    private UsuarioRequestDTO usuario;

    @Size(max = 30)
    private String perfil;
}
