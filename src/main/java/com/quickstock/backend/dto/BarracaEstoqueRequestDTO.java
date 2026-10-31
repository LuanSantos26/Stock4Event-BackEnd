package com.quickstock.backend.dto;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BarracaEstoqueRequestDTO {

    @Valid
    private List<EstoqueItemRequestDTO> itens = new ArrayList<>();
}
