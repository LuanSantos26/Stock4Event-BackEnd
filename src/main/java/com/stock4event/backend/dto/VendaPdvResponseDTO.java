package com.stock4event.backend.dto;

import com.stock4event.backend.entity.Empresa;
import com.stock4event.backend.entity.ItemPedido;
import com.stock4event.backend.entity.Pedido;
import com.stock4event.backend.entity.Produto;
import com.stock4event.backend.entity.VendaPdv;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Venda do PDV, com tudo o que o comprovante precisa.
 * Formato esperado pelo front em src/types/pdv.ts (VendaPdv). O id é o do Pedido.
 */
@Getter
public class VendaPdvResponseDTO {

    private final Long id;
    private final String numero;
    private final String criadoEm;
    private final String status;
    private final Long barracaId;
    private final String barracaNome;
    private final String operadorNome;
    private final String empresaNome;
    private final String empresaCnpj;
    private final String formaPagamento;
    private final BigDecimal total;
    private final BigDecimal valorRecebido;
    private final BigDecimal troco;
    private final String clienteNome;
    private final String clienteDocumento;
    private final List<Item> itens;

    public VendaPdvResponseDTO(VendaPdv venda, List<ItemPedido> itens) {
        Pedido pedido = venda.getPedido();
        Empresa empresa = pedido.getBarraca().getEvento().getEmpresa();
        this.id = pedido.getId();
        this.numero = numeroDaVenda(pedido.getId());
        this.criadoEm = pedido.getCriadoEm() != null ? pedido.getCriadoEm().toString() : null;
        this.status = pedido.getStatus();
        this.barracaId = pedido.getBarraca().getId();
        this.barracaNome = pedido.getBarraca().getNome();
        this.operadorNome = pedido.getOperador() != null ? pedido.getOperador().getNome() : null;
        this.empresaNome = empresa.getNome();
        this.empresaCnpj = empresa.getCnpj();
        this.formaPagamento = pedido.getMetodoPagamento();
        this.total = pedido.getValorTotal();
        this.valorRecebido = venda.getValorRecebido();
        this.troco = venda.getTroco();
        this.clienteNome = venda.getClienteNome();
        this.clienteDocumento = venda.getClienteDocumento();
        this.itens = itens.stream().map(Item::new).toList();
    }

    public static String numeroDaVenda(Long pedidoId) {
        return pedidoId != null ? String.format("PDV-%06d", pedidoId) : null;
    }

    @Getter
    public static class Item {
        private final Long produtoId;
        private final String codigo;
        private final String nome;
        private final String unidade;
        private final BigDecimal quantidade;
        private final BigDecimal precoUnitario;
        private final BigDecimal subtotal;

        public Item(ItemPedido item) {
            Produto produto = item.getProduto();
            this.produtoId = produto.getId();
            this.codigo = produto.getCodigo();
            this.nome = produto.getNome();
            this.unidade = produto.getUnidade();
            this.quantidade = item.getQuantidade();
            this.precoUnitario = item.getPrecoUnitario();
            this.subtotal = item.getSubtotal();
        }
    }
}
