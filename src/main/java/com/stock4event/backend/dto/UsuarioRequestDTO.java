package com.stock4event.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UsuarioRequestDTO {

    @NotBlank(message = "Nome é obrigatório.")
    @Size(max = 100)
    private String nome;

    @NotBlank(message = "E-mail é obrigatório.")
    @Email(message = "E-mail inválido.")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "Senha é obrigatória.")
    @Size(min = 6, max = 100, message = "Senha deve ter entre 6 e 100 caracteres.")
    private String senha;

    @Size(max = 14)
    private String cpf;
}
