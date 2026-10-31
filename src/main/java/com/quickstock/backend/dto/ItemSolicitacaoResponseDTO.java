package com.quickstock.backend.dto;

import com.quickstock.backend.entity.ItemSolicitacaoCompra;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class ItemSolicitacaoResponseDTO {

    private final Long produtoId;
    private final String nome;
    private final String unidade;
    private final BigDecimal quantidade;
    private final BigDecimal precoUnitario;
    private final BigDecimal subtotal;
    private final String imagemUrl;

    public ItemSolicitacaoResponseDTO(ItemSolicitacaoCompra item) {
        this.produtoId = item.getProduto().getId();
        this.nome = item.getProduto().getNome();
        this.unidade = item.getProduto().getUnidade();
        this.quantidade = item.getQuantidade();
        this.precoUnitario = item.getPrecoUnitario();
        this.subtotal = item.getSubtotal();
        this.imagemUrl = item.getProduto().getImagemUrl();
    }
}
