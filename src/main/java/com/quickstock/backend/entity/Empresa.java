package com.quickstock.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "empresas")
@Data
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 150)
    private String nome;

    @NotBlank
    @Size(max = 18)
    @Column(unique = true, nullable = false)
    private String cnpj;

    @Size(max = 20)
    private String telefone;

    @Size(max = 20)
    @Column(nullable = true)
    private String tipo = "COMPRADOR";

    @Size(max = 300)
    private String descricao;

    @Size(max = 500)
    @Column(name = "logo_url")
    private String logoUrl;

    @Size(max = 500)
    @Column(name = "capa_url")
    private String capaUrl;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;
}
