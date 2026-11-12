package com.stock4event.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Conta onde o fornecedor recebe os pagamentos dos pedidos (uma por empresa).
 * Mostrada ao cliente no checkout: boleto e transferência usam a conta; Pix usa a chave.
 */
@Entity
@Table(name = "dados_bancarios")
@Data
public class DadosBancarios {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empresa_id", nullable = false, unique = true)
    private Empresa empresa;

    /** Código do banco (3 dígitos, ex.: 001, 237, 260). */
    @Column(name = "banco_codigo", nullable = false, length = 3)
    private String bancoCodigo;

    @Column(name = "banco_nome", nullable = false, length = 80)
    private String bancoNome;

    @Column(nullable = false, length = 10)
    private String agencia;

    /** Número da conta com dígito, ex.: 12345-6. */
    @Column(nullable = false, length = 20)
    private String conta;

    /** corrente ou poupanca. */
    @Column(name = "tipo_conta", nullable = false, length = 20)
    private String tipoConta;

    @Column(nullable = false, length = 120)
    private String titular;

    /** CPF ou CNPJ do titular, só dígitos. */
    @Column(nullable = false, length = 14)
    private String documento;

    /** cpf, cnpj, email, telefone ou aleatoria. */
    @Column(name = "tipo_chave_pix", length = 20)
    private String tipoChavePix;

    @Column(name = "chave_pix", length = 120)
    private String chavePix;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}
