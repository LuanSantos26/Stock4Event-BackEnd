package com.stock4event.backend.dto;

import com.stock4event.backend.entity.Barraca;
import com.stock4event.backend.entity.Empresa;
import com.stock4event.backend.entity.EstoqueBarraca;
import com.stock4event.backend.entity.Evento;
import com.stock4event.backend.entity.Produto;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Quiosque (barraca) ativo de outra empresa, para a vitrine "Quiosques disponíveis".
 * Formato esperado pelo front em src/types/marketplace.ts (QuiosqueDisponivel).
 */
@Getter
public class QuiosqueDisponivelDTO {

    private final Long id;
    private final String nome;
    private final String eventoNome;
    private final String dataInicio;
    private final String dataFim;
    private final Long empresaId;
    private final String empresaNome;
    private final String empresaTipo;
    private final String empresaDescricao;
    private final String empresaLogoUrl;
    private final String empresaCapaUrl;
    /** A dona também vende no marketplace: o app pode abrir a loja dela. */
    private final boolean distribuidora;
    private final int totalProdutos;
    private final int produtosDisponiveis;
    private final List<ProdutoQuiosque> produtos;

    public QuiosqueDisponivelDTO(Barraca barraca, List<EstoqueBarraca> estoque, boolean distribuidora) {
        Evento evento = barraca.getEvento();
        Empresa empresa = evento.getEmpresa();
        this.id = barraca.getId();
        this.nome = barraca.getNome();
        this.eventoNome = evento.getNome();
        this.dataInicio = evento.getDataInicio() != null ? evento.getDataInicio().toString() : null;
        this.dataFim = evento.getDataFim() != null ? evento.getDataFim().toString() : null;
        this.empresaId = empresa.getId();
        this.empresaNome = empresa.getNome();
        this.empresaTipo = empresa.getTipo();
        this.empresaDescricao = empresa.getDescricao();
        this.empresaLogoUrl = empresa.getLogoUrl();
        this.empresaCapaUrl = empresa.getCapaUrl();
        this.distribuidora = distribuidora;
        this.produtos = estoque.stream().map(ProdutoQuiosque::new).toList();
        this.totalProdutos = this.produtos.size();
        this.produtosDisponiveis = (int) this.produtos.stream().filter(ProdutoQuiosque::isDisponivel).count();
    }

    @Getter
    public static class ProdutoQuiosque {
        private final Long produtoId;
        private final String nome;
        private final String unidade;
        private final BigDecimal precoVenda;
        private final String imagemUrl;
        private final boolean disponivel;

        public ProdutoQuiosque(EstoqueBarraca item) {
            Produto produto = item.getProduto();
            this.produtoId = produto.getId();
            this.nome = produto.getNome();
            this.unidade = produto.getUnidade();
            this.precoVenda = produto.getPrecoVenda();
            this.imagemUrl = produto.getImagemUrl();
            BigDecimal qtd = item.getQuantidade() != null ? item.getQuantidade() : BigDecimal.ZERO;
            this.disponivel = qtd.signum() > 0 && Integer.valueOf(1).equals(produto.getAtivo());
        }
    }
}
