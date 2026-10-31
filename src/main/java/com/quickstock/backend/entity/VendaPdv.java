package com.quickstock.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Dados próprios de uma venda do PDV que o {@link Pedido} não guarda.
 *
 * A venda em si é um Pedido com tipo "pdv" (itens em ItemPedido e o pagamento em Pagamento),
 * para entrar nos relatórios do FinanceiroService sem mudar nada lá.
 */
@Entity
@Table(name = "vendas_pdv")
@Data
public class VendaPdv {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private Pedido pedido;

    /** Chave gerada pelo app: reenviar a mesma chave devolve esta venda, sem duplicar. */
    @NotNull
    @Column(name = "chave_venda", nullable = false, unique = true, length = 64)
    private String chaveVenda;

    @Column(name = "valor_recebido", precision = 10, scale = 2)
    private BigDecimal valorRecebido;

    @Column(precision = 10, scale = 2)
    private BigDecimal troco;

    @Column(name = "cliente_nome", length = 120)
    private String clienteNome;

    /** CPF ou CNPJ, só dígitos. */
    @Column(name = "cliente_documento", length = 14)
    private String clienteDocumento;
}
