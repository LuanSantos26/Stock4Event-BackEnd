package com.stock4event.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Relatório de um período (dia, mês ou ano): vendas do fornecedor ou compras do cliente.
 * O app mostra o resumo e gera o PDF/CSV para baixar.
 */
public record RelatorioDTO(
        String perfil,
        String periodo,
        String titulo,
        String empresaNome,
        LocalDate inicio,
        LocalDate fim,
        LocalDateTime geradoEm,
        int totalPedidos,
        int concluidos,
        int emAndamento,
        int cancelados,
        BigDecimal valorTotal,
        BigDecimal valorConcluido,
        BigDecimal ticketMedio,
        BigDecimal unidades,
        List<ProdutoLinha> produtos,
        List<ContraparteLinha> contrapartes,
        List<PedidoLinha> pedidos
) {
    /** Produto vendido/comprado no período (pedidos não cancelados). */
    public record ProdutoLinha(String nome, BigDecimal unidades, BigDecimal valor) {}

    /** Cliente (no relatório do fornecedor) ou distribuidora (no do cliente). */
    public record ContraparteLinha(String nome, int pedidos, BigDecimal valor) {}

    public record PedidoLinha(
            Long id,
            LocalDateTime criadoEm,
            String contraparte,
            String tipoEntrega,
            String status,
            String statusLabel,
            String metodoPagamento,
            BigDecimal valor
    ) {}
}
