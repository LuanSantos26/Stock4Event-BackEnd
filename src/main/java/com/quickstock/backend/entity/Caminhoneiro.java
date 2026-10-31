package com.quickstock.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "caminhoneiros")
@Data
public class Caminhoneiro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @NotBlank
    @Size(max = 120)
    private String nome;

    @NotBlank
    @Size(max = 80)
    private String veiculo;

    @NotBlank
    @Size(max = 150)
    private String rota;

    @NotBlank
    @Size(max = 30)
    @Column(nullable = false)
    private String status = "Disponível";

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @OneToMany(mappedBy = "caminhoneiro", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<EntregaLogistica> entregas = new ArrayList<>();
}
