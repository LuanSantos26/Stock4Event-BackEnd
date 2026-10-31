package com.quickstock.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "entregas_logistica")
@Data
public class EntregaLogistica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caminhoneiro_id", nullable = false)
    private Caminhoneiro caminhoneiro;

    @NotBlank
    @Size(max = 120)
    @Column(name = "nome_cliente", nullable = false)
    private String nomeCliente;

    @NotBlank
    @Size(max = 80)
    private String cidade;

    @NotBlank
    @Size(max = 150)
    private String rota;

    @NotBlank
    @Size(max = 30)
    @Column(nullable = false)
    private String status = "Carregando";

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;
}
