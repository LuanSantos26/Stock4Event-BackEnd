package com.quickstock.backend.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NotificacaoDTO {

    private final String id;
    private final String tipo;
    private final String titulo;
    private final String mensagem;
    private final Long fornecedorId;
    private final String fornecedorNome;
    private final Long solicitacaoId;
    private final LocalDateTime criadoEm;

    public NotificacaoDTO(
            String id,
            String tipo,
            String titulo,
            String mensagem,
            Long fornecedorId,
            String fornecedorNome,
            Long solicitacaoId,
            LocalDateTime criadoEm) {
        this.id = id;
        this.tipo = tipo;
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.fornecedorId = fornecedorId;
        this.fornecedorNome = fornecedorNome;
        this.solicitacaoId = solicitacaoId;
        this.criadoEm = criadoEm;
    }
}
