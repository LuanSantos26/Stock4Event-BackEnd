package com.quickstock.backend.dto;

import java.math.BigDecimal;

public class MovimentoStockDiaDTO {

    private final String tipo;
    private final String nome;
    private final String horario;
    private final String quantidade;
    private final BigDecimal valor;
    private final String origem;

    public MovimentoStockDiaDTO(
            String tipo,
            String nome,
            String horario,
            String quantidade,
            BigDecimal valor,
            String origem) {
        this.tipo = tipo;
        this.nome = nome;
        this.horario = horario;
        this.quantidade = quantidade;
        this.valor = valor;
        this.origem = origem;
    }

    public String getTipo() { return tipo; }
    public String getNome() { return nome; }
    public String getHorario() { return horario; }
    public String getQuantidade() { return quantidade; }
    public BigDecimal getValor() { return valor; }
    public String getOrigem() { return origem; }
}
