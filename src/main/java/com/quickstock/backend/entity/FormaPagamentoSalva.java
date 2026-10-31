package com.quickstock.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Entity
@Table(name = "formas_pagamento_salvas")
@Data
public class FormaPagamentoSalva {

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

    @NotBlank
    @Size(max = 80)
    private String apelido;

    @Column(nullable = false)
    private boolean principal = false;
}
