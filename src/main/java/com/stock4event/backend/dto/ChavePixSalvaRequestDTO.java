package com.stock4event.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChavePixSalvaRequestDTO {

    @NotNull
    private Long empresaId;

    @NotBlank
    @Size(max = 80)
    private String apelido;

    @NotBlank
    @Size(max = 20)
    private String tipoChave;

    @NotBlank
    @Size(max = 60)
    private String chaveMascarada;

    @NotBlank
    @Size(max = 4)
    private String ultimosDigitos;
}
