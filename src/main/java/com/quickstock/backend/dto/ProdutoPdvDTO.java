package com.quickstock.backend.dto;

import com.quickstock.backend.entity.EstoqueBarraca;
import com.quickstock.backend.entity.Produto;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Produto vendável no PDV de um quiosque.
 * Formato esperado pelo front em src/types/pdv.ts (ProdutoPdv).
 */
@Getter
public class ProdutoPdvDTO {

    private final Long id;
    private final String codigo;
    private final String nome;
    private final String unidade;
    private final BigDecimal precoVenda;
    private final String imagemUrl;
    /** Menor valor entre o estoque do quiosque e o estoque total do produto. */
    private final BigDecimal disponivel;

    public ProdutoPdvDTO(EstoqueBarraca item) {
        Produto produto = item.getProduto();
        this.id = produto.getId();
        this.codigo = produto.getCodigo();
        this.nome = produto.getNome();
        this.unidade = produto.getUnidade();
        this.precoVenda = produto.getPrecoVenda();
        this.imagemUrl = produto.getImagemUrl();
        this.disponivel = calcularDisponivel(item);
    }

    public static BigDecimal calcularDisponivel(EstoqueBarraca item) {
        BigDecimal noQuiosque = item.getQuantidade() != null ? item.getQuantidade() : BigDecimal.ZERO;
        BigDecimal total = item.getProduto().getEstoque() != null ? item.getProduto().getEstoque() : BigDecimal.ZERO;
        BigDecimal menor = noQuiosque.min(total);
        return menor.signum() > 0 ? menor : BigDecimal.ZERO;
    }
}
