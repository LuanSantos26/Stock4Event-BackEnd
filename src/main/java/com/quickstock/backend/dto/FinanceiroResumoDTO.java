package com.quickstock.backend.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
public class FinanceiroResumoDTO {

    private final BigDecimal lucroTotal;
    private final BigDecimal lucroMesAtual;
    private final BigDecimal mediaLucroMensal;
    private final int margemLucroPercentual;
    private final int mediaPedidosMensais;
    private final int totalPedidosMesAtual;
    private final BigDecimal totalComprasMesAtual;
    private final BigDecimal mediaComprasMensais;
    private final BigDecimal totalVendasAcumulado;
    private final BigDecimal totalComprasAcumulado;
    private final List<MesLucroDTO> lucrosMensais;
    private final List<MesValorDTO> comprasMensais;
    private final List<MesValorDTO> vendasMensais;
    private final List<MesPedidosDTO> pedidosMensais;
    private final List<FormaPagamentoDTO> formasPagamento;

    public FinanceiroResumoDTO(
            BigDecimal lucroTotal,
            BigDecimal lucroMesAtual,
            BigDecimal mediaLucroMensal,
            int margemLucroPercentual,
            int mediaPedidosMensais,
            int totalPedidosMesAtual,
            BigDecimal totalComprasMesAtual,
            BigDecimal mediaComprasMensais,
            BigDecimal totalVendasAcumulado,
            BigDecimal totalComprasAcumulado,
            List<MesLucroDTO> lucrosMensais,
            List<MesValorDTO> comprasMensais,
            List<MesValorDTO> vendasMensais,
            List<MesPedidosDTO> pedidosMensais,
            List<FormaPagamentoDTO> formasPagamento) {
        this.lucroTotal = lucroTotal;
        this.lucroMesAtual = lucroMesAtual;
        this.mediaLucroMensal = mediaLucroMensal;
        this.margemLucroPercentual = margemLucroPercentual;
        this.mediaPedidosMensais = mediaPedidosMensais;
        this.totalPedidosMesAtual = totalPedidosMesAtual;
        this.totalComprasMesAtual = totalComprasMesAtual;
        this.mediaComprasMensais = mediaComprasMensais;
        this.totalVendasAcumulado = totalVendasAcumulado;
        this.totalComprasAcumulado = totalComprasAcumulado;
        this.lucrosMensais = lucrosMensais;
        this.comprasMensais = comprasMensais;
        this.vendasMensais = vendasMensais;
        this.pedidosMensais = pedidosMensais;
        this.formasPagamento = formasPagamento;
    }
}
