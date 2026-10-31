package com.quickstock.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedido")
@Data
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String tipo = "pdv";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "barraca_id")
    private Barraca barraca;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "operador_id", nullable = false)
    private Usuario operador;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empresa_compradora_id")
    private Empresa empresaCompradora;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empresa_fornecedora_id")
    private Empresa empresaFornecedora;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "endereco_entrega_id")
    private EnderecoEntrega enderecoEntrega;

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

    @Size(max = 20)
    @Column(name = "metodo_pagamento")
    private String metodoPagamento;

    @Column(name = "taxa_entrega", precision = 10, scale = 2)
    private BigDecimal taxaEntrega = BigDecimal.ZERO;

    @Size(max = 500)
    private String observacao;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String status = "aberto";

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;
}
