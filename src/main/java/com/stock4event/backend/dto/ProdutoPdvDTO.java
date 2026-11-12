package com.stock4event.backend.dto;

import com.stock4event.backend.entity.EstoqueBarraca;
import com.stock4event.backend.entity.Produto;
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
    private final String codigoBarras;
    private final String nome;
    private final String unidade;
    private final BigDecimal precoVenda;
    private final String imagemUrl;
    /** Saldo do produto neste quiosque. */
    private final BigDecimal disponivel;

    public ProdutoPdvDTO(EstoqueBarraca item) {
        Produto produto = item.getProduto();
        this.id = produto.getId();
        this.codigo = produto.getCodigo();
        this.codigoBarras = produto.getCodigoBarras();
        this.nome = produto.getNome();
        this.unidade = produto.getUnidade();
        this.precoVenda = produto.getPrecoVenda();
        this.imagemUrl = produto.getImagemUrl();
        this.disponivel = calcularDisponivel(item);
    }

    public static BigDecimal calcularDisponivel(EstoqueBarraca item) {
        BigDecimal noQuiosque = item.getQuantidade() != null ? item.getQuantidade() : BigDecimal.ZERO;
        return noQuiosque.signum() > 0 ? noQuiosque : BigDecimal.ZERO;
    }
}
