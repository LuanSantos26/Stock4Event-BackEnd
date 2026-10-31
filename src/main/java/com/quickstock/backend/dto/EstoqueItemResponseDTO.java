package com.quickstock.backend.dto;

import com.quickstock.backend.entity.EstoqueBarraca;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class EstoqueItemResponseDTO {

    private final Long produtoId;
    private final String nome;
    private final String unidade;
    private final BigDecimal precoVenda;
    private final BigDecimal quantidade;
    private final String imagemUrl;

    public EstoqueItemResponseDTO(EstoqueBarraca estoque) {
        this.produtoId = estoque.getProduto().getId();
        this.nome = estoque.getProduto().getNome();
        this.unidade = estoque.getProduto().getUnidade();
        this.precoVenda = estoque.getProduto().getPrecoVenda();
        this.quantidade = estoque.getQuantidade();
        this.imagemUrl = estoque.getProduto().getImagemUrl();
    }
}
