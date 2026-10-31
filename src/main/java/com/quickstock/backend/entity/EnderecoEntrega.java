package com.quickstock.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Entity
@Table(name = "enderecos_entrega")
@Data
public class EnderecoEntrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @NotBlank
    @Size(max = 80)
    private String apelido;

    @NotBlank
    @Size(max = 150)
    private String logradouro;

    @NotBlank
    @Size(max = 20)
    private String numero;

    @Size(max = 80)
    private String complemento;

    @NotBlank
    @Size(max = 80)
    private String bairro;

    @NotBlank
    @Size(max = 80)
    private String cidade;

    @NotBlank
    @Size(max = 2)
    private String uf;

    @NotBlank
    @Size(max = 9)
    private String cep;

    @Column(nullable = false)
    private boolean principal = false;
}
