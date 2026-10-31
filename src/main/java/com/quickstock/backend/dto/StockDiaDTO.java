package com.quickstock.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public class StockDiaDTO {

    private final String data;
    private final String dataLabel;
    private final BigDecimal totalCompras;
    private final BigDecimal totalVendas;
    private final BigDecimal lucro;
    private final int margemPercentual;
    private final int quantidadeCompras;
    private final int quantidadeVendas;
    private final List<MovimentoStockDiaDTO> movimentos;

    public StockDiaDTO(
            String data,
            String dataLabel,
            BigDecimal totalCompras,
            BigDecimal totalVendas,
            BigDecimal lucro,
            int margemPercentual,
            int quantidadeCompras,
            int quantidadeVendas,
            List<MovimentoStockDiaDTO> movimentos) {
        this.data = data;
        this.dataLabel = dataLabel;
        this.totalCompras = totalCompras;
        this.totalVendas = totalVendas;
        this.lucro = lucro;
        this.margemPercentual = margemPercentual;
        this.quantidadeCompras = quantidadeCompras;
        this.quantidadeVendas = quantidadeVendas;
        this.movimentos = movimentos;
    }

    public String getData() { return data; }
    public String getDataLabel() { return dataLabel; }
    public BigDecimal getTotalCompras() { return totalCompras; }
    public BigDecimal getTotalVendas() { return totalVendas; }
    public BigDecimal getLucro() { return lucro; }
    public int getMargemPercentual() { return margemPercentual; }
    public int getQuantidadeCompras() { return quantidadeCompras; }
    public int getQuantidadeVendas() { return quantidadeVendas; }
    public List<MovimentoStockDiaDTO> getMovimentos() { return movimentos; }
}
