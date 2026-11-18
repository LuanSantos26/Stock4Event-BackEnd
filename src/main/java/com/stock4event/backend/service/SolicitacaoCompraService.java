package com.stock4event.backend.service;

import com.stock4event.backend.dto.EnderecoEntregaDTO;
import com.stock4event.backend.dto.SolicitacaoCompraRequestDTO;
import com.stock4event.backend.dto.SolicitacaoCompraResponseDTO;
import com.stock4event.backend.dto.StatusPedidoDTO;
import com.stock4event.backend.entity.*;
import com.stock4event.backend.exception.CadastroException;
import com.stock4event.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SolicitacaoCompraService {

    private static final List<String> TIPOS_FORNECEDOR = List.of("DISTRIBUIDOR", "AMBOS");
    private static final List<String> METODOS_PAGAMENTO = List.of("pix", "credito", "debito", "boleto", "dinheiro");

    /**
     * Etapas de um pedido com entrega. O fornecedor avança até "em_rota";
     * a última ("entregue") só o cliente marca, quando confirma que recebeu.
     */
    static final List<String> FLUXO_ENTREGA = List.of(
            "aguardando_liberacao", "em_separacao", "abastecendo", "em_rota", "entregue");

    /** Etapas de um pedido com retirada no local. O cliente conclui ao retirar ("entregue"). */
    static final List<String> FLUXO_RETIRADA = List.of(
            "aguardando_liberacao", "em_separacao", "pronto_retirada", "entregue");

    @Autowired private EnderecoEntregaRepository enderecoEntregaRepository;

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
     * Avança o status sozinho pelo tempo (uma etapa a cada segundos-por-etapa), para a demo
     * funcionar com distribuidoras que não têm usuário. Com false, só o fornecedor muda o status.
     */
    @Value("${stock4event.pedidos.status-automatico:true}")
    private boolean statusAutomatico;

    /** Na demonstração automática, cada etapa avança depois deste tempo (o fornecedor pode adiantar). */
    @Value("${stock4event.pedidos.segundos-por-etapa:60}")
    private long segundosPorEtapa;

    public List<SolicitacaoCompraResponseDTO> listarRecebidas(Long empresaFornecedoraId) {
        return solicitacaoRepository.findByEmpresaFornecedoraIdOrderByCriadoEmDesc(empresaFornecedoraId).stream()
                .map(this::atualizarStatusDemonstracao)
                .map(this::toResponse)
                .toList();
    }

    /**
     * Fornecedor avança o pedido uma etapa por vez (separação → carro → rota) ou cancela.
     * A conclusão ("entregue") é do cliente: ver {@link #concluirPeloComprador}.
     */
    @Transactional
    public SolicitacaoCompraResponseDTO atualizarStatus(Long id, Long empresaFornecedoraId, String novoStatus) {
        SolicitacaoCompra solicitacao = buscar(id);
        if (!solicitacao.getEmpresaFornecedora().getId().equals(empresaFornecedoraId)) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Pedido não pertence ao fornecedor informado.");
        }

        String atual = statusAtual(solicitacao);
        String status = novoStatus == null ? "" : novoStatus.trim().toLowerCase();
        // Nomes usados por versões antigas do app.
        if ("preparando".equals(status)) status = "em_separacao";
        if ("despachado".equals(status)) status = "em_rota";
        if ("entregue".equals(status)) {
            throw new CadastroException(
                    HttpStatus.BAD_REQUEST,
                    ehRetirada(solicitacao)
                            ? "Quem conclui a retirada é o cliente, quando buscar os produtos."
                            : "Quem conclui a entrega é o cliente, quando receber os produtos."
            );
        }
        List<String> fluxo = fluxo(solicitacao);
        int iAtual = fluxo.indexOf(atual);
        boolean finalizado = "entregue".equals(atual) || "cancelada".equals(atual);
        boolean permitido;
        if ("cancelada".equals(status)) {
            permitido = !finalizado;
        } else {
            // O fornecedor avança uma etapa por vez, sem pular nenhuma.
            int iNovo = fluxo.indexOf(status);
            permitido = !finalizado && iAtual >= 0 && iNovo == iAtual + 1;
        }
        if (!permitido) {
            throw new CadastroException(
                    HttpStatus.BAD_REQUEST,
                    "Não é possível mudar o pedido de \"" + labelStatus(atual, solicitacao) + "\" para \"" + labelStatus(status, solicitacao) + "\"."
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
        String atualComprador = statusAtual(solicitacao);
        if (!"aguardando_liberacao".equals(atualComprador) && !"em_separacao".equals(atualComprador)) {
            throw new CadastroException(
                    HttpStatus.BAD_REQUEST,
                    "Só é possível cancelar enquanto o pedido está sendo separado."
            );
        }

        return toResponse(aplicarStatus(solicitacao, "cancelada"));
    }

    /**
     * Último passo do pedido: o cliente confirma que recebeu (entrega) ou retirou (retirada).
     * Só vale quando o fornecedor já deixou o pedido em rota ou pronto para retirada.
     */
    @Transactional
    public SolicitacaoCompraResponseDTO concluirPeloComprador(Long id, Long empresaCompradoraId) {
        SolicitacaoCompra solicitacao = buscar(id);
        if (!solicitacao.getEmpresaCompradora().getId().equals(empresaCompradoraId)) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Pedido não pertence à empresa informada.");
        }

        solicitacao = atualizarStatusDemonstracao(solicitacao);
        String atual = statusAtual(solicitacao);
        if ("entregue".equals(atual)) {
            return toResponse(solicitacao); // já concluído: não faz nada (toque duplo)
        }
        if (!etapaAntesDaConclusao(solicitacao).equals(atual)) {
            throw new CadastroException(
                    HttpStatus.BAD_REQUEST,
                    ehRetirada(solicitacao)
                            ? "Só dá para concluir quando o pedido estiver pronto para retirada."
                            : "Só dá para concluir quando o pedido estiver a caminho."
            );
        }

        return toResponse(aplicarStatus(solicitacao, "entregue"));
    }

    /** Etapa em que o pedido espera a confirmação do cliente: em_rota ou pronto_retirada. */
    private static String etapaAntesDaConclusao(SolicitacaoCompra solicitacao) {
        List<String> fluxo = fluxo(solicitacao);
        return fluxo.get(fluxo.size() - 2);
    }

    private SolicitacaoCompra buscar(Long id) {
        return solicitacaoRepository.findById(id)
                .orElseThrow(() -> new CadastroException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));
    }

    private static boolean ehRetirada(SolicitacaoCompra solicitacao) {
        return "retirada".equals(solicitacao.getTipoEntrega());
    }

    private static List<String> fluxo(SolicitacaoCompra solicitacao) {
        return ehRetirada(solicitacao) ? FLUXO_RETIRADA : FLUXO_ENTREGA;
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

        if (("entregue".equals(status) || "cancelada".equals(status)) && solicitacao.getConcluidoEm() == null) {
            solicitacao.setConcluidoEm(LocalDateTime.now());
            solicitacao = solicitacaoRepository.save(solicitacao);
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

        boolean retirada = "retirada".equalsIgnoreCase(
                dto.getTipoEntrega() != null ? dto.getTipoEntrega().trim() : "");

        EnderecoEntrega endereco;
        String enderecoResumo;
        BigDecimal taxaEntrega;
        if (retirada) {
            // Retirada no local: o endereço é o do fornecedor (o principal, se ele tiver cadastrado).
            endereco = enderecoEntregaRepository
                    .findByEmpresaIdOrderByPrincipalDescApelidoAsc(fornecedora.getId())
                    .stream().findFirst().orElse(null);
            enderecoResumo = "Retirada no local: " + (endereco != null
                    ? EnderecoEntregaDTO.formatarResumo(endereco)
                    : fornecedora.getNome());
            taxaEntrega = BigDecimal.ZERO;
        } else {
            if (dto.getEnderecoEntregaId() == null) {
                throw new CadastroException(HttpStatus.BAD_REQUEST, "Informe o endereço de entrega.");
            }
            endereco = enderecoEntregaService.validarEnderecoDaEmpresa(
                    dto.getEnderecoEntregaId(), compradora.getId());
            enderecoResumo = EnderecoEntregaDTO.formatarResumo(endereco);
            taxaEntrega = dto.getTaxaEntrega() != null ? dto.getTaxaEntrega() : BigDecimal.ZERO;
        }

        SolicitacaoCompra solicitacao = new SolicitacaoCompra();
        solicitacao.setEmpresaCompradora(compradora);
        solicitacao.setEmpresaFornecedora(fornecedora);
        solicitacao.setUsuarioSolicitante(usuario);
        solicitacao.setObservacao(dto.getObservacao());
        solicitacao.setMetodoPagamento(metodoPagamento);
        solicitacao.setEnderecoResumo(enderecoResumo);
        if (endereco != null) {
            solicitacao.setCep(endereco.getCep());
            solicitacao.setLogradouro(endereco.getLogradouro());
            solicitacao.setNumero(endereco.getNumero());
            solicitacao.setComplemento(endereco.getComplemento());
            solicitacao.setBairro(endereco.getBairro());
            solicitacao.setCidade(endereco.getCidade());
            solicitacao.setUf(endereco.getUf());
        }
        solicitacao.setTipoEntrega(retirada ? "retirada" : "entrega");
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

            boolean porPalete = "palete".equalsIgnoreCase(
                    itemDto.getTipoVenda() != null ? itemDto.getTipoVenda().trim() : "");
            BigDecimal unidades = itemDto.getQuantidade();
            BigDecimal precoUnitario = produto.getPrecoVenda();
            BigDecimal subtotal;
            Integer quantidadePaletes = null;
            if (porPalete) {
                if (produto.getPrecoPalete() == null || produto.getUnidadesPorPalete() == null
                        || produto.getUnidadesPorPalete() <= 0) {
                    throw new CadastroException(
                            HttpStatus.BAD_REQUEST,
                            "O produto " + produto.getNome() + " não é vendido em palete fechado."
                    );
                }
                if (itemDto.getQuantidade().stripTrailingZeros().scale() > 0) {
                    throw new CadastroException(HttpStatus.BAD_REQUEST, "A quantidade de paletes deve ser um número inteiro.");
                }
                quantidadePaletes = itemDto.getQuantidade().intValueExact();
                unidades = BigDecimal.valueOf((long) quantidadePaletes * produto.getUnidadesPorPalete());
                subtotal = produto.getPrecoPalete().multiply(BigDecimal.valueOf(quantidadePaletes));
                precoUnitario = produto.getPrecoPalete()
                        .divide(BigDecimal.valueOf(produto.getUnidadesPorPalete()), 2, RoundingMode.HALF_UP);
            } else {
                subtotal = produto.getPrecoVenda().multiply(unidades);
            }

            BigDecimal estoqueAtual = produto.getEstoque() != null ? produto.getEstoque() : BigDecimal.ZERO;
            if (estoqueAtual.compareTo(unidades) < 0) {
                throw new CadastroException(
                        HttpStatus.BAD_REQUEST,
                        "Estoque insuficiente para \"" + produto.getNome() + "\". Disponível: "
                                + estoqueAtual.stripTrailingZeros().toPlainString()
                );
            }

            estoqueProdutoService.debitarEstoque(produto, unidades);

            ItemSolicitacaoCompra item = new ItemSolicitacaoCompra();
            item.setSolicitacao(solicitacao);
            item.setProduto(produto);
            item.setQuantidade(unidades);
            item.setPrecoUnitario(precoUnitario);
            item.setSubtotal(subtotal);
            item.setTipoVenda(porPalete ? "palete" : "unidade");
            item.setQuantidadePaletes(quantidadePaletes);
            item.setPrecoPalete(porPalete ? produto.getPrecoPalete() : null);
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
                enderecoResumo,
                retirada,
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
            String enderecoResumo,
            boolean retirada,
            String metodoPagamento,
            BigDecimal taxaEntrega,
            BigDecimal valorTotal,
            List<ItemSolicitacaoCompra> itensSolicitacao) {

        Pedido pedido = new Pedido();
        pedido.setTipo("marketplace");
        pedido.setOperador(usuario);
        pedido.setEmpresaCompradora(compradora);
        pedido.setEmpresaFornecedora(fornecedora);
        pedido.setEnderecoResumo(enderecoResumo);
        if (endereco != null) {
            // Na retirada o endereço é do fornecedor: fica só no resumo, não como endereço de entrega.
            if (!retirada) {
                pedido.setEnderecoEntrega(endereco);
            }
            pedido.setCep(endereco.getCep());
            pedido.setLogradouro(endereco.getLogradouro());
            pedido.setNumero(endereco.getNumero());
            pedido.setComplemento(endereco.getComplemento());
            pedido.setBairro(endereco.getBairro());
            pedido.setCidade(endereco.getCidade());
            pedido.setUf(endereco.getUf());
        }
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
            // Boleto fica aguardando compensação; na demonstração o pedido segue normalmente.
            case "boleto", "dinheiro" -> "pendente";
            default -> "pendente";
        };
    }

    private SolicitacaoCompra atualizarStatusDemonstracao(SolicitacaoCompra solicitacao) {
        if (!statusAutomatico || solicitacao.getCriadoEm() == null) return solicitacao;

        long segundos = Duration.between(solicitacao.getCriadoEm(), LocalDateTime.now()).getSeconds();
        List<String> fluxo = fluxo(solicitacao);
        int iAtual = fluxo.indexOf(statusAtual(solicitacao));
        if (iAtual < 0) return solicitacao; // cancelada ou status antigo

        // Só avança (nunca volta): se o fornecedor já adiantou uma etapa, ela é mantida.
        // Para antes da última: "entregue" é sempre o cliente que confirma.
        int iDevido = (int) Math.min(fluxo.size() - 2, segundos / Math.max(1, segundosPorEtapa));
        for (int i = iAtual + 1; i <= iDevido; i++) {
            solicitacao = aplicarStatus(solicitacao, fluxo.get(i));
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
                labelStatus(status, solicitacao),
                montarEtapas(status, solicitacao),
                previsaoMinutos,
                formatarPrevisaoEntrega(status, previsaoMinutos, ehRetirada(solicitacao))
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

    private String formatarPrevisaoEntrega(String status, int minutos, boolean retirada) {
        if (retirada) {
            return switch (status) {
                case "entregue" -> "Retirada concluída";
                case "cancelada" -> "Pedido cancelado";
                case "pronto_retirada" -> "Pronto! Retire no local e toque em Concluir retirada";
                default -> "Avisaremos quando estiver pronto para retirada";
            };
        }
        return switch (status) {
            case "entregue" -> "Entrega concluída";
            case "cancelada" -> "Pedido cancelado";
            case "em_rota" -> "Chegada em até " + minutos + " min. Ao receber, toque em Concluir entrega";
            default -> {
                int max = minutos + 15;
                yield "Previsão de entrega: " + minutos + "–" + max + " min";
            }
        };
    }

    private String labelStatus(String status, SolicitacaoCompra solicitacao) {
        boolean retirada = ehRetirada(solicitacao);
        return switch (status) {
            case "aguardando_liberacao" -> "Aguardando liberação da distribuidora";
            case "em_separacao" -> "Produto em separação";
            case "abastecendo" -> "Abastecendo o carro";
            case "em_rota" -> "A caminho, em rota de entrega";
            case "pronto_retirada" -> "Pronto para retirada no local";
            case "entregue" -> retirada ? "Retirada concluída pelo cliente" : "Entrega concluída pelo cliente";
            case "cancelada" -> "Pedido cancelado";
            default -> "Pedido em andamento";
        };
    }

    private List<StatusPedidoDTO> montarEtapas(String status, SolicitacaoCompra solicitacao) {
        boolean retirada = ehRetirada(solicitacao);
        List<String> fluxo = fluxo(solicitacao);
        List<String> labels = retirada
                ? List.of("Pedido confirmado", "Produto em separação", "Pronto para retirada", "Retirada concluída")
                : List.of("Pedido confirmado", "Produto em separação", "Abastecendo o carro",
                        "A caminho / em rota", "Entrega concluída");

        // A primeira etapa ("Pedido confirmado") já vale assim que o pedido existe.
        int indiceAtivo;
        if ("cancelada".equals(status)) {
            indiceAtivo = -2;
        } else if ("entregue".equals(status)) {
            indiceAtivo = -1;
        } else {
            indiceAtivo = Math.max(1, fluxo.indexOf(status));
        }

        List<StatusPedidoDTO> etapas = new ArrayList<>();
        for (int i = 0; i < fluxo.size(); i++) {
            String codigo = i == 0 ? "pedido_efetuado" : fluxo.get(i);
            // Cancelado: só a confirmação fica concluída e nenhuma etapa fica ativa.
            boolean concluida = indiceAtivo == -1 || (indiceAtivo == -2 ? i == 0 : i < indiceAtivo);
            boolean ativa = i == indiceAtivo;
            etapas.add(new StatusPedidoDTO(codigo, labels.get(i), i + 1, concluida, ativa));
        }
        return etapas;
    }
}
