package com.quickstock.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BarracaRequestDTO {

    @NotBlank
    @Size(max = 100)
    private String nome;

    @NotNull
    private Long empresaId;

    @NotNull
    private Long responsavelId;

    private List<EstoqueItemRequestDTO> itens = new ArrayList<>();
}
