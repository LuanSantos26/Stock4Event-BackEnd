package com.quickstock.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitacoes_compra")
@Data
public class SolicitacaoCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empresa_compradora_id", nullable = false)
    private Empresa empresaCompradora;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empresa_fornecedora_id", nullable = false)
    private Empresa empresaFornecedora;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_solicitante_id", nullable = false)
    private Usuario usuarioSolicitante;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String status = "enviada";

    @Size(max = 500)
    private String observacao;

    @Size(max = 20)
    @Column(name = "metodo_pagamento")
    private String metodoPagamento;

    @Size(max = 300)
    @Column(name = "endereco_resumo")
    private String enderecoResumo;

    @Size(max = 9)
    private String cep;

    @Size(max = 150)
    private String logradouro;

    @Size(max = 20)
    private String numero;

    @Size(max = 80)
    private String complemento;

    @Size(max = 80)
    private String bairro;

    @Size(max = 80)
    private String cidade;

    @Size(max = 2)
    private String uf;

    @Column(name = "taxa_entrega", precision = 10, scale = 2)
    private BigDecimal taxaEntrega = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "estoque_comprador_creditado", nullable = false)
    private Boolean estoqueCompradorCreditado = false;
}
