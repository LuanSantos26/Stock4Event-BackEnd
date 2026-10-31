package com.quickstock.backend.dto;

import com.quickstock.backend.entity.Empresa;
import com.quickstock.backend.entity.Produto;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Produto de uma distribuidora para a vitrine "Adicionados pelos fornecedores".
 * Formato esperado pelo front em src/types/marketplace.ts (ProdutoRecente).
 */
@Getter
public class ProdutoRecenteDTO {

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
    private final String fornecedorNome;
    private final String fornecedorLogoUrl;
    private final String fornecedorCapaUrl;
    private final String fornecedorDescricao;
    private final String fornecedorTipo;
    /** A entidade Produto ainda não guarda data de criação; fica nulo até a coluna existir. */
    private final String criadoEm;
    /** Produto da própria empresa do usuário logado: aparece, mas não pode ser comprado. */
    private final boolean proprio;

    public ProdutoRecenteDTO(Produto produto, Long empresaUsuarioId) {
        Empresa empresa = produto.getEmpresa();
        this.id = produto.getId();
        this.empresaId = empresa.getId();
        this.nome = produto.getNome();
        this.precoVenda = produto.getPrecoVenda();
        this.unidade = produto.getUnidade();
        this.descricao = produto.getDescricao();
        this.imagemUrl = produto.getImagemUrl();
        this.ativo = produto.getAtivo();
        this.estoque = produto.getEstoque();
        this.codigo = produto.getCodigo();
        this.fornecedorNome = empresa.getNome();
        this.fornecedorLogoUrl = empresa.getLogoUrl();
        this.fornecedorCapaUrl = empresa.getCapaUrl();
        this.fornecedorDescricao = empresa.getDescricao();
        this.fornecedorTipo = empresa.getTipo();
        this.criadoEm = null;
        this.proprio = empresaUsuarioId != null && empresaUsuarioId.equals(empresa.getId());
    }
}
