package com.stock4event.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "itens_solicitacao_compra")
@Data
public class ItemSolicitacaoCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "solicitacao_id", nullable = false)
    private SolicitacaoCompra solicitacao;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @NotNull
    @DecimalMin("0.001")
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantidade;

    @NotNull
    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @NotNull
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    /** "unidade" ou "palete". A quantidade acima é sempre em unidades (é o que mexe no estoque). */
    @Column(name = "tipo_venda", length = 10)
    private String tipoVenda = "unidade";

    /** Quantos paletes fechados foram comprados (só quando tipoVenda = "palete"). */
    @Column(name = "quantidade_paletes")
    private Integer quantidadePaletes;

    /** Preço de um palete na hora da compra (só quando tipoVenda = "palete"). */
    @Column(name = "preco_palete", precision = 10, scale = 2)
    private BigDecimal precoPalete;
}
