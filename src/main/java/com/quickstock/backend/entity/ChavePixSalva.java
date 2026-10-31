package com.quickstock.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chaves_pix_salvas")
@Data
public class ChavePixSalva {

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
    @Size(max = 20)
    @Column(name = "tipo_chave", nullable = false)
    private String tipoChave;

    // Só a versão mascarada é guardada, como nos cartões.
    @NotBlank
    @Size(max = 60)
    @Column(name = "chave_mascarada", nullable = false)
    private String chaveMascarada;

    @NotBlank
    @Size(max = 4)
    @Column(name = "ultimos_digitos", nullable = false)
    private String ultimosDigitos;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;
}
