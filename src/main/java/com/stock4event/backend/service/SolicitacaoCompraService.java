package com.stock4event.backend.service;

import com.stock4event.backend.dto.EnderecoEntregaDTO;
import com.stock4event.backend.dto.SolicitacaoCompraRequestDTO;
import com.stock4event.backend.dto.SolicitacaoCompraResponseDTO;
import com.stock4event.backend.dto.StatusPedidoDTO;
import com.stock4event.backend.entity.*;
import com.stock4event.backend.exception.CadastroException;
import com.stock4event.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SolicitacaoCompraService {

    private static final List<String> TIPOS_FORNECEDOR = List.of("DISTRIBUIDOR", "AMBOS");
    private static final List<String> METODOS_PAGAMENTO = List.of("pix", "credito", "debito", "dinheiro");

    // ── Status do pedido ───────────────────────────────────────────────────
    // As quatro etapas são percorridas uma a uma, sempre pela mão do fornecedor:
    // recebido → preparando → em_rota → entregue. Nada avança sozinho.
    public static final String STATUS_RECEBIDO = "recebido";
    public static final String STATUS_PREPARANDO = "preparando";
    public static final String STATUS_EM_ROTA = "em_rota";
    public static final String STATUS_ENTREGUE = "entregue";
    public static final String STATUS_CANCELADA = "cancelada";

    /** Etapa seguinte de cada status; o fim da fila (entregue) não tem sucessor. */
    private static final Map<String, String> PROXIMO_STATUS = Map.of(
            STATUS_RECEBIDO, STATUS_PREPARANDO,
            STATUS_PREPARANDO, STATUS_EM_ROTA,
            STATUS_EM_ROTA, STATUS_ENTREGUE
    );

    /** Enquanto o pedido não saiu para entrega, ainda dá para cancelar. */
    private static final List<String> STATUS_CANCELAVEIS =
            List.of(STATUS_RECEBIDO, STATUS_PREPARANDO, STATUS_EM_ROTA);

    @Autowired private SolicitacaoCompraRepository solicitacaoRepository;
    @Autowired private ItemSolicitacaoCompraRepository itemRepository;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private EnderecoEntregaService enderecoEntregaService;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private ItemPedidoRepository itemPedidoRepository;
    @Autowired private PagamentoRepository pagamentoRepository;
    @Autowired private EstoqueProdutoService estoqueProdutoService;

    public List<SolicitacaoCompraResponseDTO> listarRecebidas(Long empresaFornecedoraId) {
        return solicitacaoRepository.findByEmpresaFornecedoraIdOrderByCriadoEmDesc(empresaFornecedoraId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Fornecedor move o pedido para a etapa seguinte, ou cancela antes da entrega.
     * Só aceita o próximo passo da fila: não dá para pular etapa nem voltar atrás.
     */
    @Transactional
    public SolicitacaoCompraResponseDTO atualizarStatus(Long id, Long empresaFornecedoraId, String novoStatus) {
        SolicitacaoCompra solicitacao = buscar(id);
        if (!solicitacao.getEmpresaFornecedora().getId().equals(empresaFornecedoraId)) {
            throw new CadastroException(HttpStatus.FORBIDDEN, "Pedido não pertence ao fornecedor informado.");
        }

        String atual = statusAtual(solicitacao);
        String status = novoStatus == null ? "" : novoStatus.trim().toLowerCase();

        boolean permitido = STATUS_CANCELADA.equals(status)
                ? STATUS_CANCELAVEIS.contains(atual)
                : status.equals(PROXIMO_STATUS.get(atual));

        if (!permitido) {
            throw new CadastroException(
                    HttpStatus.BAD_REQUEST,
                    "Não é possível mudar o pedido de \"" + labelStatus(atual) + "\" para \"" + labelStatus(status) + "\"."
            );
        }

        return toResponse(aplicarStatus(solicitacao, status));
    }

    /** Comprador cancela enquanto o pedido ainda não saiu para entrega. */
    @Transactional
    public SolicitacaoCompraResponseDTO cancelarPeloComprador(Long id, Long empresaCompradoraId) {
        SolicitacaoCompra solicitacao = buscar(id);
        if (!solicitacao.getEmpresaCompradora().getId().equals(empresaCompradoraId)) {
            throw new CadastroException(HttpStatus.FORBIDDEN, "Pedido não pertence à empresa informada.");
        }

        String atual = statusAtual(solicitacao);
        // O comprador só cancela antes do despacho; depois disso a mercadoria já saiu.
        if (!STATUS_RECEBIDO.equals(atual) && !STATUS_PREPARANDO.equals(atual)) {
            throw new CadastroException(
                    HttpStatus.BAD_REQUEST,
                    "Só é possível cancelar antes de o pedido sair para entrega."
            );
        }

        return toResponse(aplicarStatus(solicitacao, STATUS_CANCELADA));
    }

    private SolicitacaoCompra buscar(Long id) {
        return solicitacaoRepository.findById(id)
                .orElseThrow(() -> new CadastroException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));
    }

    private String statusAtual(SolicitacaoCompra solicitacao) {
        return solicitacao.getStatus() != null ? solicitacao.getStatus() : STATUS_RECEBIDO;
    }

    /** Grava o novo status na solicitação e no pedido, e aplica os efeitos de estoque e pagamento. */
    private SolicitacaoCompra aplicarStatus(SolicitacaoCompra solicitacao, String status) {
        solicitacao.setStatus(status);
        solicitacao = solicitacaoRepository.save(solicitacao);

        Pedido pedido = solicitacao.getPedido();
        if (pedido != null) {
            pedido.setStatus(status);
            pedidoRepository.save(pedido);
        }

        if (STATUS_ENTREGUE.equals(status)) {
            estoqueProdutoService.creditarCompradorSeNecessario(solicitacao);
        }

        if (STATUS_CANCELADA.equals(status)) {
            estoqueProdutoService.devolverEstoqueFornecedor(solicitacao);
            if (pedido != null) {
                for (Pagamento pagamento : pagamentoRepository.findByPedidoId(pedido.getId())) {
                    pagamento.setStatus("aprovado".equals(pagamento.getStatus()) ? "estornado" : "cancelado");
                    pagamentoRepository.save(pagamento);
                }
            }
        }

        return solicitacao;
    }

    public List<SolicitacaoCompraResponseDTO> listarPorComprador(Long empresaCompradoraId) {
        return solicitacaoRepository.findByEmpresaCompradoraIdOrderByCriadoEmDesc(empresaCompradoraId).stream()
                .map(s -> {
                    if (STATUS_ENTREGUE.equals(s.getStatus())) {
                        estoqueProdutoService.creditarCompradorSeNecessario(s);
                    }
                    return toResponse(s);
                })
                .toList();
    }

    public SolicitacaoCompraResponseDTO obterPorId(Long id, Long empresaCompradoraId) {
        SolicitacaoCompra solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new CadastroException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));

        if (!solicitacao.getEmpresaCompradora().getId().equals(empresaCompradoraId)) {
            throw new CadastroException(HttpStatus.FORBIDDEN, "Pedido não pertence à empresa informada.");
        }

        if (STATUS_ENTREGUE.equals(solicitacao.getStatus())) {
            estoqueProdutoService.creditarCompradorSeNecessario(solicitacao);
        }

        return toResponse(solicitacao);
    }

    @Transactional
    public SolicitacaoCompraResponseDTO criar(SolicitacaoCompraRequestDTO dto) {
        if (dto.getItens() == null || dto.getItens().isEmpty()) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Informe ao menos um item na solicitação.");
        }

        Empresa compradora = empresaRepository.findById(dto.getEmpresaCompradoraId())
                .orElseThrow(() -> new CadastroException(HttpStatus.BAD_REQUEST, "Empresa compradora não encontrada."));

        Empresa fornecedora = empresaRepository.findById(dto.getEmpresaFornecedoraId())
                .orElseThrow(() -> new CadastroException(HttpStatus.BAD_REQUEST, "Fornecedor não encontrado."));

        if (!TIPOS_FORNECEDOR.contains(fornecedora.getTipo())) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Empresa informada não é fornecedora.");
        }

        if (compradora.getId().equals(fornecedora.getId())) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Não é possível solicitar compra da própria empresa.");
        }

        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new CadastroException(HttpStatus.BAD_REQUEST, "Usuário não encontrado."));

        if (!usuario.getEmpresa().getId().equals(compradora.getId())) {
            throw new CadastroException(HttpStatus.FORBIDDEN, "Usuário não pertence à empresa compradora.");
        }

        String metodoPagamento = dto.getMetodoPagamento() != null
                ? dto.getMetodoPagamento().trim().toLowerCase()
                : "";
        if (!METODOS_PAGAMENTO.contains(metodoPagamento)) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Forma de pagamento inválida.");
        }

        if (dto.getEnderecoEntregaId() == null) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Informe o endereço de entrega.");
        }

        EnderecoEntrega endereco = enderecoEntregaService.validarEnderecoDaEmpresa(
                dto.getEnderecoEntregaId(), compradora.getId());

        BigDecimal taxaEntrega = dto.getTaxaEntrega() != null ? dto.getTaxaEntrega() : BigDecimal.ZERO;

        SolicitacaoCompra solicitacao = new SolicitacaoCompra();
        solicitacao.setEmpresaCompradora(compradora);
        solicitacao.setEmpresaFornecedora(fornecedora);
        solicitacao.setUsuarioSolicitante(usuario);
        solicitacao.setObservacao(dto.getObservacao());
        solicitacao.setMetodoPagamento(metodoPagamento);
        solicitacao.setEnderecoResumo(EnderecoEntregaDTO.formatarResumo(endereco));
        solicitacao.setCep(endereco.getCep());
        solicitacao.setLogradouro(endereco.getLogradouro());
        solicitacao.setNumero(endereco.getNumero());
        solicitacao.setComplemento(endereco.getComplemento());
        solicitacao.setBairro(endereco.getBairro());
        solicitacao.setCidade(endereco.getCidade());
        solicitacao.setUf(endereco.getUf());
        solicitacao.setTaxaEntrega(taxaEntrega);
        solicitacao.setStatus(STATUS_RECEBIDO);
        solicitacao.setValorTotal(BigDecimal.ZERO);
        solicitacao = solicitacaoRepository.save(solicitacao);

        BigDecimal total = BigDecimal.ZERO;

        for (var itemDto : dto.getItens()) {
            Produto produto = produtoRepository.findById(itemDto.getProdutoId())
                    .orElseThrow(() -> new CadastroException(
                            HttpStatus.BAD_REQUEST,
                            "Produto não encontrado: " + itemDto.getProdutoId()
                    ));

            if (!produto.getEmpresa().getId().equals(fornecedora.getId())) {
                throw new CadastroException(HttpStatus.BAD_REQUEST, "Produto não pertence ao fornecedor selecionado.");
            }

            BigDecimal estoqueAtual = produto.getEstoque() != null ? produto.getEstoque() : BigDecimal.ZERO;
            if (estoqueAtual.compareTo(itemDto.getQuantidade()) < 0) {
                throw new CadastroException(
                        HttpStatus.BAD_REQUEST,
                        "Estoque insuficiente para \"" + produto.getNome() + "\". Disponível: "
                                + estoqueAtual.stripTrailingZeros().toPlainString()
                );
            }

            estoqueProdutoService.debitarEstoque(produto, itemDto.getQuantidade());

            BigDecimal subtotal = produto.getPrecoVenda().multiply(itemDto.getQuantidade());

            ItemSolicitacaoCompra item = new ItemSolicitacaoCompra();
            item.setSolicitacao(solicitacao);
            item.setProduto(produto);
            item.setQuantidade(itemDto.getQuantidade());
            item.setPrecoUnitario(produto.getPrecoVenda());
            item.setSubtotal(subtotal);
            itemRepository.save(item);

            total = total.add(subtotal);
        }

        solicitacao.setValorTotal(total.add(taxaEntrega));
        solicitacao = solicitacaoRepository.save(solicitacao);

        Pedido pedido = criarPedidoMarketplace(
                dto,
                compradora,
                fornecedora,
                usuario,
                endereco,
                metodoPagamento,
                taxaEntrega,
                solicitacao.getValorTotal(),
                itemRepository.findBySolicitacaoId(solicitacao.getId())
        );
        solicitacao.setPedido(pedido);
        solicitacao = solicitacaoRepository.save(solicitacao);

        return toResponse(solicitacao);
    }

    private Pedido criarPedidoMarketplace(
            SolicitacaoCompraRequestDTO dto,
            Empresa compradora,
            Empresa fornecedora,
            Usuario usuario,
            EnderecoEntrega endereco,
            String metodoPagamento,
            BigDecimal taxaEntrega,
            BigDecimal valorTotal,
            List<ItemSolicitacaoCompra> itensSolicitacao) {

        Pedido pedido = new Pedido();
        pedido.setTipo("marketplace");
        pedido.setOperador(usuario);
        pedido.setEmpresaCompradora(compradora);
        pedido.setEmpresaFornecedora(fornecedora);
        pedido.setEnderecoEntrega(endereco);
        pedido.setEnderecoResumo(EnderecoEntregaDTO.formatarResumo(endereco));
        pedido.setCep(endereco.getCep());
        pedido.setLogradouro(endereco.getLogradouro());
        pedido.setNumero(endereco.getNumero());
        pedido.setComplemento(endereco.getComplemento());
        pedido.setBairro(endereco.getBairro());
        pedido.setCidade(endereco.getCidade());
        pedido.setUf(endereco.getUf());
        pedido.setMetodoPagamento(metodoPagamento);
        pedido.setTaxaEntrega(taxaEntrega);
        pedido.setObservacao(dto.getObservacao());
        pedido.setStatus(STATUS_RECEBIDO);
        pedido.setValorTotal(valorTotal);
        pedido = pedidoRepository.save(pedido);

        for (ItemSolicitacaoCompra itemSolicitacao : itensSolicitacao) {
            ItemPedido itemPedido = new ItemPedido();
            itemPedido.setPedido(pedido);
            itemPedido.setProduto(itemSolicitacao.getProduto());
            itemPedido.setQuantidade(itemSolicitacao.getQuantidade());
            itemPedido.setPrecoUnitario(itemSolicitacao.getPrecoUnitario());
            itemPedido.setSubtotal(itemSolicitacao.getSubtotal());
            itemPedidoRepository.save(itemPedido);
        }

        Pagamento pagamento = new Pagamento();
        pagamento.setPedido(pedido);
        pagamento.setMetodo(metodoPagamento);
        pagamento.setValor(valorTotal);
        pagamento.setReferenciaPagamento(dto.getPagamentoReferencia());
        pagamento.setStatus(statusPagamentoSimulado(metodoPagamento));
        pagamentoRepository.save(pagamento);

        return pedido;
    }

    private String statusPagamentoSimulado(String metodoPagamento) {
        return switch (metodoPagamento) {
            case "pix", "credito", "debito" -> "aprovado";
            case "dinheiro" -> "pendente";
            default -> "pendente";
        };
    }

    private SolicitacaoCompraResponseDTO toResponse(SolicitacaoCompra solicitacao) {
        List<ItemSolicitacaoCompra> itens = itemRepository.findBySolicitacaoId(solicitacao.getId());
        String status = statusAtual(solicitacao);
        int previsaoMinutos = calcularPrevisaoMinutos(status, solicitacao.getCriadoEm());
        return new SolicitacaoCompraResponseDTO(
                solicitacao,
                itens,
                labelStatus(status),
                montarEtapas(status),
                previsaoMinutos,
                formatarPrevisaoEntrega(status, previsaoMinutos)
        );
    }

    /**
     * Estimativa mostrada ao comprador. Como o fornecedor move cada etapa na mão,
     * não existe contagem regressiva real: é só uma faixa por etapa.
     */
    private int calcularPrevisaoMinutos(String status, LocalDateTime criadoEm) {
        return switch (status) {
            case STATUS_ENTREGUE, STATUS_CANCELADA -> 0;
            case STATUS_EM_ROTA -> 15;
            case STATUS_PREPARANDO -> 35;
            default -> 50;
        };
    }

    private String formatarPrevisaoEntrega(String status, int minutos) {
        return switch (status) {
            case STATUS_ENTREGUE -> "Pedido entregue";
            case STATUS_CANCELADA -> "Pedido cancelado";
            case STATUS_EM_ROTA -> "Chegada em até " + minutos + " min";
            default -> "Previsão de entrega: " + minutos + "–" + (minutos + 15) + " min";
        };
    }

    private String labelStatus(String status) {
        return switch (status) {
            case STATUS_RECEBIDO -> "Pedido recebido";
            case STATUS_PREPARANDO -> "Preparando pedido";
            case STATUS_EM_ROTA -> "Pedido saiu para a entrega";
            case STATUS_ENTREGUE -> "Entregue";
            case STATUS_CANCELADA -> "Pedido cancelado";
            default -> "Pedido em andamento";
        };
    }

    /** As quatro etapas da linha do tempo, na ordem em que o fornecedor as percorre. */
    private List<StatusPedidoDTO> montarEtapas(String status) {
        record EtapaDef(String codigo, String label) {}
        List<EtapaDef> defs = List.of(
                new EtapaDef(STATUS_RECEBIDO, "Pedido recebido"),
                new EtapaDef(STATUS_PREPARANDO, "Preparando pedido"),
                new EtapaDef(STATUS_EM_ROTA, "Pedido saiu para a entrega"),
                new EtapaDef(STATUS_ENTREGUE, "Entregue")
        );

        int indiceAtivo = switch (status) {
            case STATUS_RECEBIDO -> 0;
            case STATUS_PREPARANDO -> 1;
            case STATUS_EM_ROTA -> 2;
            case STATUS_ENTREGUE -> -1;   // tudo concluído
            case STATUS_CANCELADA -> -2;  // parou no caminho
            default -> 0;
        };

        List<StatusPedidoDTO> etapas = new ArrayList<>();
        for (int i = 0; i < defs.size(); i++) {
            EtapaDef def = defs.get(i);
            // Cancelado: só a confirmação fica concluída e nenhuma etapa fica ativa.
            boolean concluida = indiceAtivo == -1 || (indiceAtivo == -2 ? i == 0 : i < indiceAtivo);
            boolean ativa = i == indiceAtivo;
            etapas.add(new StatusPedidoDTO(def.codigo(), def.label(), i + 1, concluida, ativa));
        }
        return etapas;
    }
}
