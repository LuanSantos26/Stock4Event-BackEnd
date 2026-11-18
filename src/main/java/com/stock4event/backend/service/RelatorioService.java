package com.stock4event.backend.service;

import com.stock4event.backend.dto.RelatorioDTO;
import com.stock4event.backend.dto.RelatorioDTO.ContraparteLinha;
import com.stock4event.backend.dto.RelatorioDTO.PedidoLinha;
import com.stock4event.backend.dto.RelatorioDTO.ProdutoLinha;
import com.stock4event.backend.entity.Empresa;
import com.stock4event.backend.entity.ItemSolicitacaoCompra;
import com.stock4event.backend.entity.SolicitacaoCompra;
import com.stock4event.backend.exception.CadastroException;
import com.stock4event.backend.repository.EmpresaRepository;
import com.stock4event.backend.repository.ItemSolicitacaoCompraRepository;
import com.stock4event.backend.repository.SolicitacaoCompraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Relatórios de vendas (fornecedor) e de compras (cliente) por dia, mês ou ano. */
@Service
public class RelatorioService {

    /** Pedidos criados só para encher os gráficos da demo: ficam fora dos relatórios. */
    private static final String OBS_SEED = "SEED_FINANCEIRO_DEMO";
    private static final Locale PT_BR = new Locale("pt", "BR");

    @Autowired private SolicitacaoCompraRepository solicitacaoRepository;
    @Autowired private ItemSolicitacaoCompraRepository itemRepository;
    @Autowired private EmpresaRepository empresaRepository;

    @Transactional(readOnly = true)
    public RelatorioDTO gerar(Long empresaId, String perfil, String periodo, LocalDate data) {
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new CadastroException(HttpStatus.BAD_REQUEST, "Empresa não encontrada."));

        boolean fornecedor = "fornecedor".equalsIgnoreCase(perfil != null ? perfil.trim() : "");
        String tipoPeriodo = periodo != null ? periodo.trim().toLowerCase() : "dia";
        LocalDate referencia = data != null ? data : LocalDate.now();

        LocalDate inicio;
        LocalDate fimExclusivo;
        String quando;
        switch (tipoPeriodo) {
            case "mes" -> {
                inicio = referencia.withDayOfMonth(1);
                fimExclusivo = inicio.plusMonths(1);
                quando = inicio.getMonth().getDisplayName(TextStyle.FULL, PT_BR) + " de " + inicio.getYear();
            }
            case "ano" -> {
                inicio = referencia.withDayOfYear(1);
                fimExclusivo = inicio.plusYears(1);
                quando = String.valueOf(inicio.getYear());
            }
            case "dia" -> {
                inicio = referencia;
                fimExclusivo = referencia.plusDays(1);
                quando = String.format("%02d/%02d/%d", referencia.getDayOfMonth(), referencia.getMonthValue(), referencia.getYear());
            }
            default -> throw new CadastroException(HttpStatus.BAD_REQUEST, "Período inválido: use dia, mes ou ano.");
        }

        LocalDateTime desde = inicio.atStartOfDay();
        LocalDateTime ate = fimExclusivo.atStartOfDay();
        List<SolicitacaoCompra> todas = fornecedor
                ? solicitacaoRepository.findByEmpresaFornecedoraIdOrderByCriadoEmDesc(empresaId)
                : solicitacaoRepository.findByEmpresaCompradoraIdOrderByCriadoEmDesc(empresaId);
        List<SolicitacaoCompra> doPeriodo = todas.stream()
                .filter(s -> s.getCriadoEm() != null
                        && !s.getCriadoEm().isBefore(desde) && s.getCriadoEm().isBefore(ate))
                .filter(s -> !OBS_SEED.equals(s.getObservacao()))
                .toList();

        int concluidos = 0;
        int cancelados = 0;
        BigDecimal valorTotal = BigDecimal.ZERO;
        BigDecimal valorConcluido = BigDecimal.ZERO;
        BigDecimal unidades = BigDecimal.ZERO;
        Map<String, BigDecimal[]> porProduto = new LinkedHashMap<>();
        Map<String, Object[]> porContraparte = new LinkedHashMap<>();
        List<PedidoLinha> pedidos = new ArrayList<>();

        for (SolicitacaoCompra s : doPeriodo) {
            String status = s.getStatus() != null ? s.getStatus() : "aguardando_liberacao";
            BigDecimal valor = s.getValorTotal() != null ? s.getValorTotal() : BigDecimal.ZERO;
            String contraparte = fornecedor ? s.getEmpresaCompradora().getNome() : s.getEmpresaFornecedora().getNome();
            boolean cancelado = "cancelada".equals(status);

            pedidos.add(new PedidoLinha(s.getId(), s.getCriadoEm(), contraparte,
                    s.getTipoEntrega() != null ? s.getTipoEntrega() : "entrega",
                    status, labelStatus(status, s.getTipoEntrega()), s.getMetodoPagamento(), valor));

            if (cancelado) {
                cancelados++;
                continue;
            }
            valorTotal = valorTotal.add(valor);
            if ("entregue".equals(status)) {
                concluidos++;
                valorConcluido = valorConcluido.add(valor);
            }

            Object[] c = porContraparte.computeIfAbsent(contraparte, k -> new Object[]{0, BigDecimal.ZERO});
            c[0] = (Integer) c[0] + 1;
            c[1] = ((BigDecimal) c[1]).add(valor);

            for (ItemSolicitacaoCompra item : itemRepository.findBySolicitacaoId(s.getId())) {
                String nome = item.getProduto() != null ? item.getProduto().getNome() : "Produto";
                BigDecimal qtd = item.getQuantidade() != null ? item.getQuantidade() : BigDecimal.ZERO;
                BigDecimal sub = item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;
                BigDecimal[] p = porProduto.computeIfAbsent(nome, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                p[0] = p[0].add(qtd);
                p[1] = p[1].add(sub);
                unidades = unidades.add(qtd);
            }
        }

        int validos = doPeriodo.size() - cancelados;
        BigDecimal ticketMedio = validos > 0
                ? valorTotal.divide(BigDecimal.valueOf(validos), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<ProdutoLinha> produtos = porProduto.entrySet().stream()
                .map(e -> new ProdutoLinha(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .sorted(Comparator.comparing(ProdutoLinha::valor).reversed())
                .toList();
        List<ContraparteLinha> contrapartes = porContraparte.entrySet().stream()
                .map(e -> new ContraparteLinha(e.getKey(), (Integer) e.getValue()[0], (BigDecimal) e.getValue()[1]))
                .sorted(Comparator.comparing(ContraparteLinha::valor).reversed())
                .toList();

        String titulo = (fornecedor ? "Vendas" : "Compras") + " — " + quando;
        return new RelatorioDTO(
                fornecedor ? "fornecedor" : "cliente",
                tipoPeriodo,
                titulo,
                empresa.getNome(),
                inicio,
                fimExclusivo.minusDays(1),
                LocalDateTime.now(),
                doPeriodo.size(),
                concluidos,
                validos - concluidos,
                cancelados,
                valorTotal,
                valorConcluido,
                ticketMedio,
                unidades,
                produtos,
                contrapartes,
                pedidos
        );
    }

    private static String labelStatus(String status, String tipoEntrega) {
        boolean retirada = "retirada".equals(tipoEntrega);
        return switch (status) {
            case "aguardando_liberacao" -> "Pedido recebido";
            case "em_separacao" -> "Em separação";
            case "abastecendo" -> "Abastecendo o carro";
            case "em_rota" -> "A caminho";
            case "pronto_retirada" -> "Pronto para retirada";
            case "entregue" -> retirada ? "Retirada concluída" : "Entrega concluída";
            case "cancelada" -> "Cancelado";
            default -> "Em andamento";
        };
    }
}
