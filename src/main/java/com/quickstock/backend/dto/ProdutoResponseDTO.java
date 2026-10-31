package com.quickstock.backend.dto;

import com.quickstock.backend.entity.Produto;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class ProdutoResponseDTO {

    private final Long id;
    private final Long empresaId;
    private final String nome;
    private final BigDecimal precoVenda;
    private final String unidade;
    private final String descricao;
    private final String imagemUrl;
    private final Integer ativo;
    private final BigDecimal estoque;
    private final String codigo;
    private final String codigoOrigem;

    public ProdutoResponseDTO(Produto produto) {
        this.id = produto.getId();
        this.empresaId = produto.getEmpresa().getId();
        this.nome = produto.getNome();
        this.precoVenda = produto.getPrecoVenda();
        this.unidade = produto.getUnidade();
        this.descricao = produto.getDescricao();
        this.imagemUrl = produto.getImagemUrl();
        this.ativo = produto.getAtivo();
        this.estoque = produto.getEstoque() != null ? produto.getEstoque() : BigDecimal.ZERO;
        this.codigo = produto.getCodigo();
        this.codigoOrigem = produto.getCodigoOrigem();
    }
}
