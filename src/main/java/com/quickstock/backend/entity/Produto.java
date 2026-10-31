package com.quickstock.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "produtos")
@Data
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @NotBlank
    @Size(max = 300, message = "Nome do produto deve ter no máximo 300 caracteres.")
    @Column(length = 300)
    private String nome;

    @NotNull
    @DecimalMin("0.0")
    @Column(name = "preco_venda", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoVenda;

    @NotBlank
    @Size(max = 20)
    private String unidade;

    @Size(max = 500)
    private String descricao;

    @Size(max = 500)
    @Column(name = "imagem_url")
    private String imagemUrl;

    @Column(nullable = false)
    private Integer ativo = 1;

    @DecimalMin("0.0")
    @Column(precision = 10, scale = 3)
    private BigDecimal estoque;

    @NotBlank
    @Size(max = 40)
    @Column(unique = true, nullable = false, length = 40)
    private String codigo;

    @Size(max = 40)
    @Column(name = "codigo_origem", length = 40)
    private String codigoOrigem;
}
