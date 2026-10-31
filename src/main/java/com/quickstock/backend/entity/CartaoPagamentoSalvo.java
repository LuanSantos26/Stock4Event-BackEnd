package com.quickstock.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "cartoes_pagamento_salvos")
@Data
public class CartaoPagamentoSalvo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @NotBlank
    @Size(max = 20)
    private String tipo;

    @Size(max = 80)
    private String apelido;

    @NotBlank
    @Size(max = 40)
    private String bandeira;

    @NotBlank
    @Size(max = 4)
    @Column(name = "ultimos_digitos", nullable = false)
    private String ultimosDigitos;

    @NotBlank
    @Size(max = 24)
    @Column(name = "numero_mascarado", nullable = false)
    private String numeroMascarado;

    @NotBlank
    @Size(max = 5)
    private String validade;

    @NotBlank
    @Size(max = 120)
    private String titular;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;
}
