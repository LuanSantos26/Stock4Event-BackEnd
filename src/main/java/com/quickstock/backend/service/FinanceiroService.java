package com.quickstock.backend.service;

import com.quickstock.backend.dto.*;
import com.quickstock.backend.entity.Pagamento;
import com.quickstock.backend.entity.Pedido;
import com.quickstock.backend.entity.SolicitacaoCompra;
import com.quickstock.backend.exception.CadastroException;
import com.quickstock.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FinanceiroService {

    private static final String OBS_SEED = "SEED_FINANCEIRO_DEMO";
    private static final DateTimeFormatter MES_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String[] LABELS_PT = {
            "Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"
    };

    private static final Map<String, String> LABELS_PAGAMENTO = Map.of(
            "pix", "PIX",
            "credito", "Crédito",
            "debito", "Débito",
            "dinheiro", "Dinheiro"
    );

    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private SolicitacaoCompraRepository solicitacaoRepository;
    @Autowired private ItemSolicitacaoCompraRepository itemSolicitacaoRepository;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private ItemPedidoRepository itemPedidoRepository;
    @Autowired private PagamentoRepository pagamentoRepository;

    public FinanceiroResumoDTO obterResumo(Long empresaCompradoraId) {
        empresaRepository.findById(empresaCompradoraId)
                .orElseThrow(() -> new CadastroException(HttpStatus.NOT_FOUND, "Empresa não encontrada."));

        List<YearMonth> meses = ultimosMeses(6);
        YearMonth inicio = meses.get(0);
        LocalDateTime desde = inicio.atDay(1).atStartOfDay();
        LocalDateTime ate = meses.get(meses.size() - 1).plusMonths(1).atDay(1).atStartOfDay();

        List<SolicitacaoCompra> comprasPeriodo = solicitacaoRepository
                .findComprasPorEmpresaNoPeriodo(empresaCompradoraId, desde, ate, OBS_SEED);
        List<Pedido> vendasPeriodo = pedidoRepository
                .findVendasPorEmpresaNoPeriodo(empresaCompradoraId, desde, ate);

        Map<YearMonth, BigDecimal> comprasPorMes = agruparComprasPorMes(comprasPeriodo);
        Map<YearMonth, BigDecimal> vendasPorMes = agruparVendasPorMes(vendasPeriodo);
        Map<YearMonth, Long> pedidosPorMes = agruparContagemPedidosPorMes(vendasPeriodo);

        List<MesLucroDTO> lucrosMensais = new ArrayList<>();
        List<MesValorDTO> comprasMensais = new ArrayList<>();
        List<MesValorDTO> vendasMensais = new ArrayList<>();
        List<MesPedidosDTO> pedidosMensais = new ArrayList<>();

        BigDecimal somaLucro = BigDecimal.ZERO;
        BigDecimal somaCompras = BigDecimal.ZERO;
        BigDecimal somaVendas = BigDecimal.ZERO;
        int somaPedidos = 0;

        for (YearMonth ym : meses) {
            String mes = ym.format(MES_FMT);
            String label = LABELS_PT[ym.getMonthValue() - 1];

            BigDecimal compra = comprasPorMes.getOrDefault(ym, BigDecimal.ZERO);
            BigDecimal venda = vendasPorMes.getOrDefault(ym, BigDecimal.ZERO);
            BigDecimal lucro = venda.subtract(compra);

            comprasMensais.add(new MesValorDTO(mes, label, compra));
            vendasMensais.add(new MesValorDTO(mes, label, venda));
            lucrosMensais.add(new MesLucroDTO(mes, label, lucro, compra));

            int pedidos = pedidosPorMes.getOrDefault(ym, 0L).intValue();
            pedidosMensais.add(new MesPedidosDTO(mes, label, pedidos));

            somaLucro = somaLucro.add(lucro);
            somaCompras = somaCompras.add(compra);
            somaVendas = somaVendas.add(venda);
            somaPedidos += pedidos;
        }

        int qtdMeses = meses.size();
        BigDecimal lucroMesAtual = lucrosMensais.get(qtdMeses - 1).getLucro();
        BigDecimal vendasMesAtual = vendasPorMes.getOrDefault(meses.get(qtdMeses - 1), BigDecimal.ZERO);
        BigDecimal totalComprasMesAtual = comprasMensais.get(qtdMeses - 1).getValor();
        int totalPedidosMesAtual = pedidosMensais.get(qtdMeses - 1).getQuantidade();

        BigDecimal mediaLucro = somaLucro.divide(bd(String.valueOf(qtdMeses)), 2, RoundingMode.HALF_UP);
        BigDecimal mediaCompras = somaCompras.divide(bd(String.valueOf(qtdMeses)), 2, RoundingMode.HALF_UP);
        int mediaPedidos = Math.round((float) somaPedidos / qtdMeses);

        int margem = vendasMesAtual.compareTo(BigDecimal.ZERO) > 0
                ? lucroMesAtual.multiply(bd("100"))
                        .divide(vendasMesAtual, 0, RoundingMode.HALF_UP)
                        .intValue()
                : 0;

        List<FormaPagamentoDTO> formasPagamento = montarFormasPagamentoReais(
                empresaCompradoraId, desde, ate);

        return new FinanceiroResumoDTO(
                somaLucro.setScale(2, RoundingMode.HALF_UP),
                lucroMesAtual.setScale(2, RoundingMode.HALF_UP),
                mediaLucro,
                margem,
                mediaPedidos,
                totalPedidosMesAtual,
                totalComprasMesAtual,
                mediaCompras,
                somaVendas.setScale(2, RoundingMode.HALF_UP),
                somaCompras.setScale(2, RoundingMode.HALF_UP),
                lucrosMensais,
                comprasMensais,
                vendasMensais,
                pedidosMensais,
                formasPagamento
        );
    }

    public StockDiaDTO obterStockDia(Long empresaCompradoraId) {
        empresaRepository.findById(empresaCompradoraId)
                .orElseThrow(() -> new CadastroException(HttpStatus.NOT_FOUND, "Empresa não encontrada."));

        LocalDate hoje = LocalDate.now();
        LocalDateTime inicio = hoje.atStartOfDay();
        LocalDateTime fim = hoje.plusDays(1).atStartOfDay();
        DateTimeFormatter horaFmt = DateTimeFormatter.ofPattern("HH:mm");

        List<SolicitacaoCompra> comprasHoje = solicitacaoRepository
                .findComprasPorEmpresaNoPeriodo(empresaCompradoraId, inicio, fim, OBS_SEED);

        BigDecimal totalCompras = comprasHoje.stream()
                .map(this::valorCompra)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<MovimentoStockDiaDTO> movimentos = new ArrayList<>();

        for (SolicitacaoCompra solicitacao : comprasHoje) {
            String fornecedor = solicitacao.getEmpresaFornecedora() != null
                    ? solicitacao.getEmpresaFornecedora().getNome()
                    : "Fornecedor";
            String horario = solicitacao.getCriadoEm().format(horaFmt);

            itemSolicitacaoRepository.findBySolicitacaoId(solicitacao.getId()).forEach(item -> {
                String nome = item.getProduto() != null ? item.getProduto().getNome() : "Produto";
                String unidade = item.getProduto() != null ? item.getProduto().getUnidade() : "UN";
                movimentos.add(new MovimentoStockDiaDTO(
                        "compra",
                        nome,
                        horario,
                        item.getQuantidade().stripTrailingZeros().toPlainString() + " " + unidade,
                        item.getSubtotal(),
                        fornecedor
                ));
            });
        }

        List<Pedido> pedidosHoje = pedidoRepository
                .findVendasPorEmpresaNoPeriodo(empresaCompradoraId, inicio, fim);

        BigDecimal totalVendas = pedidosHoje.stream()
                .map(Pedido::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (Pedido pedido : pedidosHoje) {
            String barraca = pedido.getBarraca() != null ? pedido.getBarraca().getNome() : "Barraca";
            String horario = pedido.getCriadoEm().format(horaFmt);

            itemPedidoRepository.findByPedidoId(pedido.getId()).forEach(item -> {
                String nome = item.getProduto() != null ? item.getProduto().getNome() : "Produto";
                String unidade = item.getProduto() != null ? item.getProduto().getUnidade() : "UN";
                movimentos.add(new MovimentoStockDiaDTO(
                        "venda",
                        nome,
                        horario,
                        item.getQuantidade().stripTrailingZeros().toPlainString() + " " + unidade,
                        item.getSubtotal(),
                        barraca
                ));
            });
        }

        BigDecimal lucro = totalVendas.subtract(totalCompras);
        int margem = totalVendas.compareTo(BigDecimal.ZERO) > 0
                ? lucro.multiply(bd("100")).divide(totalVendas, 0, RoundingMode.HALF_UP).intValue()
                : 0;

        String dataLabel = capitalize(
                hoje.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("pt", "BR"))
        ) + ", " + hoje.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        movimentos.sort(Comparator.comparing(MovimentoStockDiaDTO::getHorario));

        return new StockDiaDTO(
                hoje.toString(),
                dataLabel,
                totalCompras,
                totalVendas,
                lucro,
                margem,
                comprasHoje.size(),
                pedidosHoje.size(),
                movimentos
        );
    }

    private BigDecimal valorCompra(SolicitacaoCompra s) {
        BigDecimal taxa = s.getTaxaEntrega() != null ? s.getTaxaEntrega() : BigDecimal.ZERO;
        return s.getValorTotal().add(taxa);
    }

    private Map<YearMonth, BigDecimal> agruparComprasPorMes(List<SolicitacaoCompra> compras) {
        return compras.stream()
                .filter(s -> s.getCriadoEm() != null)
                .collect(Collectors.groupingBy(
                        s -> YearMonth.from(s.getCriadoEm()),
                        Collectors.reducing(BigDecimal.ZERO, this::valorCompra, BigDecimal::add)
                ));
    }

    private Map<YearMonth, BigDecimal> agruparVendasPorMes(List<Pedido> pedidos) {
        return pedidos.stream()
                .filter(p -> p.getCriadoEm() != null)
                .collect(Collectors.groupingBy(
                        p -> YearMonth.from(p.getCriadoEm()),
                        Collectors.reducing(BigDecimal.ZERO, Pedido::getValorTotal, BigDecimal::add)
                ));
    }

    private Map<YearMonth, Long> agruparContagemPedidosPorMes(List<Pedido> pedidos) {
        return pedidos.stream()
                .filter(p -> p.getCriadoEm() != null)
                .collect(Collectors.groupingBy(
                        p -> YearMonth.from(p.getCriadoEm()),
                        Collectors.counting()
                ));
    }

    private List<FormaPagamentoDTO> montarFormasPagamentoReais(
            Long empresaId, LocalDateTime desde, LocalDateTime ate) {
        List<Pagamento> pagamentos = pagamentoRepository.findPorEmpresaNoPeriodo(empresaId, desde, ate);
        if (pagamentos.isEmpty()) {
            return List.of();
        }

        Map<String, BigDecimal> porMetodo = new LinkedHashMap<>();
        for (Pagamento pg : pagamentos) {
            String metodo = pg.getMetodo() != null ? pg.getMetodo().trim().toLowerCase() : "outro";
            porMetodo.merge(metodo, pg.getValor(), BigDecimal::add);
        }

        BigDecimal total = porMetodo.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return List.of();
        }

        List<FormaPagamentoDTO> resultado = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : porMetodo.entrySet()) {
            String metodo = entry.getKey();
            BigDecimal valor = entry.getValue();
            int pct = valor.multiply(bd("100"))
                    .divide(total, 0, RoundingMode.HALF_UP)
                    .intValue();
            String label = LABELS_PAGAMENTO.getOrDefault(metodo, capitalize(metodo));
            resultado.add(new FormaPagamentoDTO(metodo, label, pct, valor.setScale(2, RoundingMode.HALF_UP)));
        }
        return resultado;
    }

    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private List<YearMonth> ultimosMeses(int qtd) {
        YearMonth atual = YearMonth.now();
        List<YearMonth> lista = new ArrayList<>();
        for (int i = qtd - 1; i >= 0; i--) {
            lista.add(atual.minusMonths(i));
        }
        return lista;
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
