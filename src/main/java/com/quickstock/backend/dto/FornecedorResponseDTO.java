package com.quickstock.backend.dto;

import com.quickstock.backend.entity.Empresa;
import lombok.Getter;

@Getter
public class FornecedorResponseDTO {

    private final Long id;
    private final String nome;
    private final String descricao;
    private final String tipo;
    private final String logoUrl;
    private final String capaUrl;
    private final int totalProdutos;

    public FornecedorResponseDTO(Empresa empresa, int totalProdutos) {
        this.id = empresa.getId();
        this.nome = empresa.getNome();
        this.descricao = empresa.getDescricao();
        this.tipo = empresa.getTipo();
        this.logoUrl = empresa.getLogoUrl();
        this.capaUrl = empresa.getCapaUrl();
        this.totalProdutos = totalProdutos;
    }
}
