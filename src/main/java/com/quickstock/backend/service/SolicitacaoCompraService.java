package com.quickstock.backend.service;

import com.quickstock.backend.dto.EnderecoEntregaDTO;
import com.quickstock.backend.dto.SolicitacaoCompraRequestDTO;
import com.quickstock.backend.dto.SolicitacaoCompraResponseDTO;
import com.quickstock.backend.dto.StatusPedidoDTO;
import com.quickstock.backend.entity.*;
import com.quickstock.backend.exception.CadastroException;
import com.quickstock.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SolicitacaoCompraService {

    private static final List<String> TIPOS_FORNECEDOR = List.of("DISTRIBUIDOR", "AMBOS");
    private static final List<String> METODOS_PAGAMENTO = List.of("pix", "credito", "debito", "dinheiro");
    private static final long SEGUNDOS_PARA_ROTA = 20L;
    private static final long SEGUNDOS_PARA_ENTREGA = 55L;

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

    /**
     * Avança o status sozinho pelo tempo (20 s → em rota, 55 s → entregue), para a demo
     * funcionar com distribuidoras que não têm usuário. Com false, só o fornecedor muda o status.
     */
    @Value("${quickstock.pedidos.status-automatico:true}")
    private boolean statusAutomatico;

    public List<SolicitacaoCompraResponseDTO> listarRecebidas(Long empresaFornecedoraId) {
        return solicitacaoRepository.findByEmpresaFornecedoraIdOrderByCriadoEmDesc(empresaFornecedoraId).stream()
                .map(this::atualizarStatusDemonstracao)
                .map(this::toResponse)
                .toList();
    }

    /** Fornecedor avança o pedido (em_rota → entregue) ou cancela antes da entrega. */
    @Transactional
    public SolicitacaoCompraResponseDTO atualizarStatus(Long id, Long empresaFornecedoraId, String novoStatus) {
        SolicitacaoCompra solicitacao = buscar(id);
        if (!solicitacao.getEmpresaFornecedora().getId().equals(empresaFornecedoraId)) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Pedido não pertence ao fornecedor informado.");
        }

        String atual = statusAtual(solicitacao);
        String status = novoStatus == null ? "" : novoStatus.trim().toLowerCase();
        boolean permitido = switch (status) {
            case "em_rota" -> "aguardando_liberacao".equals(atual);
            case "entregue" -> "em_rota".equals(atual);
            case "cancelada" -> "aguardando_liberacao".equals(atual) || "em_rota".equals(atual);
            default -> false;
        };
        if (!permitido) {
            throw new CadastroException(
                    HttpStatus.BAD_REQUEST,
                    "Não é possível mudar o pedido de \"" + labelStatus(atual) + "\" para \"" + status + "\"."
            );
        }

        return toResponse(aplicarStatus(solicitacao, status));
    }

    /** Comprador cancela enquanto o pedido ainda não saiu para entrega. */
    @Transactional
    public SolicitacaoCompraResponseDTO cancelarPeloComprador(Long id, Long empresaCompradoraId) {
        SolicitacaoCompra solicitacao = buscar(id);
        if (!solicitacao.getEmpresaCompradora().getId().equals(empresaCompradoraId)) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Pedido não pertence à empresa informada.");
        }

        solicitacao = atualizarStatusDemonstracao(solicitacao);
        if (!"aguardando_liberacao".equals(statusAtual(solicitacao))) {
            throw new CadastroException(
                    HttpStatus.BAD_REQUEST,
                    "Só é possível cancelar antes de o pedido sair para entrega."
            );
        }

        return toResponse(aplicarStatus(solicitacao, "cancelada"));
    }

    private SolicitacaoCompra buscar(Long id) {
        return solicitacaoRepository.findById(id)
                .orElseThrow(() -> new CadastroException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));
    }

    private String statusAtual(SolicitacaoCompra solicitacao) {
        return solicitacao.getStatus() != null ? solicitacao.getStatus() : "aguardando_liberacao";
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

        if ("entregue".equals(status)) {
            estoqueProdutoService.creditarCompradorSeNecessario(solicitacao);
        }

        if ("cancelada".equals(status)) {
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
                    SolicitacaoCompra atualizada = atualizarStatusDemonstracao(s);
                    if ("entregue".equals(atualizada.getStatus())) {
                        estoqueProdutoService.creditarCompradorSeNecessario(atualizada);
                    }
                    return toResponse(atualizada);
                })
                .toList();
    }

    public SolicitacaoCompraResponseDTO obterPorId(Long id, Long empresaCompradoraId) {
        SolicitacaoCompra solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new CadastroException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));

        if (!solicitacao.getEmpresaCompradora().getId().equals(empresaCompradoraId)) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Pedido não pertence à empresa informada.");
        }

        solicitacao = atualizarStatusDemonstracao(solicitacao);
        if ("entregue".equals(solicitacao.getStatus())) {
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
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Usuário não pertence à empresa compradora.");
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
        solicitacao.setStatus("aguardando_liberacao");
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
        pedido.setStatus("aguardando_liberacao");
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

    private SolicitacaoCompra atualizarStatusDemonstracao(SolicitacaoCompra solicitacao) {
        if (!statusAutomatico || solicitacao.getCriadoEm() == null) return solicitacao;

        long segundos = Duration.between(solicitacao.getCriadoEm(), LocalDateTime.now()).getSeconds();
        String status = statusAtual(solicitacao);

        if ("aguardando_liberacao".equals(status) && segundos >= SEGUNDOS_PARA_ROTA) {
            solicitacao = aplicarStatus(solicitacao, "em_rota");
            status = "em_rota";
        }

        if ("em_rota".equals(status) && segundos >= SEGUNDOS_PARA_ENTREGA) {
            solicitacao = aplicarStatus(solicitacao, "entregue");
        }

        return solicitacao;
    }

    private SolicitacaoCompraResponseDTO toResponse(SolicitacaoCompra solicitacao) {
        List<ItemSolicitacaoCompra> itens = itemRepository.findBySolicitacaoId(solicitacao.getId());
        String status = solicitacao.getStatus() != null ? solicitacao.getStatus() : "aguardando_liberacao";
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

    private int calcularPrevisaoMinutos(String status, LocalDateTime criadoEm) {
        if ("entregue".equals(status) || "cancelada".equals(status)) {
            return 0;
        }
        if ("em_rota".equals(status)) {
            return 12 + (criadoEm != null ? (int) (criadoEm.getMinute() % 13) : 5);
        }
        if (criadoEm == null) {
            return 50;
        }
        long minutosDecorridos = Duration.between(criadoEm, LocalDateTime.now()).toMinutes();
        return (int) Math.max(20, 55 - minutosDecorridos);
    }

    private String formatarPrevisaoEntrega(String status, int minutos) {
        return switch (status) {
            case "entregue" -> "Pedido entregue";
            case "cancelada" -> "Pedido cancelado";
            case "em_rota" -> "Chegada em até " + minutos + " min";
            default -> {
                int max = minutos + 15;
                yield "Previsão de entrega: " + minutos + "–" + max + " min";
            }
        };
    }

    private String labelStatus(String status) {
        return switch (status) {
            case "aguardando_liberacao" -> "Aguardando liberação da distribuidora";
            case "em_rota" -> "Saindo para rota de entrega";
            case "entregue" -> "Pedido entregue";
            case "cancelada" -> "Pedido cancelado";
            default -> "Pedido em andamento";
        };
    }

    private List<StatusPedidoDTO> montarEtapas(String status) {
        record EtapaDef(String codigo, String label) {}
        List<EtapaDef> defs = List.of(
                new EtapaDef("pedido_efetuado", "Pedido confirmado"),
                new EtapaDef("aguardando_liberacao", "Preparando pedido"),
                new EtapaDef("em_rota", "Saiu para entrega"),
                new EtapaDef("entregue", "Pedido entregue")
        );

        int indiceAtivo = switch (status) {
            case "aguardando_liberacao" -> 1;
            case "em_rota" -> 2;
            case "entregue" -> -1;
            case "cancelada" -> -2;
            default -> 1;
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
