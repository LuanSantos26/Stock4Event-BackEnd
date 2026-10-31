package com.quickstock.backend.service;

import com.quickstock.backend.dto.ProdutoPdvDTO;
import com.quickstock.backend.dto.VendaPdvRequestDTO;
import com.quickstock.backend.dto.VendaPdvResponseDTO;
import com.quickstock.backend.entity.Barraca;
import com.quickstock.backend.entity.EstoqueBarraca;
import com.quickstock.backend.entity.ItemPedido;
import com.quickstock.backend.entity.Pagamento;
import com.quickstock.backend.entity.Pedido;
import com.quickstock.backend.entity.Produto;
import com.quickstock.backend.entity.Usuario;
import com.quickstock.backend.entity.VendaPdv;
import com.quickstock.backend.exception.CadastroException;
import com.quickstock.backend.repository.BarracaRepository;
import com.quickstock.backend.repository.EstoqueBarracaRepository;
import com.quickstock.backend.repository.ItemPedidoRepository;
import com.quickstock.backend.repository.PagamentoRepository;
import com.quickstock.backend.repository.PedidoRepository;
import com.quickstock.backend.repository.ProdutoRepository;
import com.quickstock.backend.repository.UsuarioRepository;
import com.quickstock.backend.repository.VendaPdvRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Ponto de venda (PDV) dos quiosques: lista os produtos vendáveis e registra vendas.
 *
 * Cada venda vira um Pedido com tipo "pdv" (itens em ItemPedido e pagamento em Pagamento), que é
 * de onde o FinanceiroService já lê as vendas. O que só o PDV usa (chave, troco, cliente) fica em VendaPdv.
 *
 * Regras:
 * - o preço vem do cadastro do produto (o app não manda preço nem total);
 * - a venda dá baixa no estoque do quiosque e no estoque total do produto;
 * - reenviar a mesma chaveVenda devolve a venda já registrada, sem duplicar;
 * - com login, só a empresa dona do quiosque pode vender nele.
 */
@Service
public class PdvService {

    static final String STATUS_CONCLUIDO = "concluido";
    static final Set<String> FORMAS_PAGAMENTO = Set.of("pix", "credito", "debito", "dinheiro");

    @Autowired private BarracaRepository barracaRepository;
    @Autowired private EstoqueBarracaRepository estoqueBarracaRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private VendaPdvRepository vendaPdvRepository;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private ItemPedidoRepository itemPedidoRepository;
    @Autowired private PagamentoRepository pagamentoRepository;

    @Transactional(readOnly = true)
    public List<ProdutoPdvDTO> listarProdutos(Long barracaId, String busca, Long usuarioId) {
        Barraca barraca = buscarBarracaAutorizada(barracaId, usuarioId);
        String termo = busca == null ? "" : busca.trim().toLowerCase(Locale.ROOT);

        return estoqueBarracaRepository.findByBarracaId(barraca.getId()).stream()
                .filter(e -> Integer.valueOf(1).equals(e.getProduto().getAtivo()))
                .filter(e -> termo.isEmpty() || corresponde(e.getProduto(), termo))
                .sorted(Comparator.comparing(e -> e.getProduto().getNome(), String.CASE_INSENSITIVE_ORDER))
                .map(ProdutoPdvDTO::new)
                .toList();
    }

    @Transactional
    public VendaPdvResponseDTO registrarVenda(VendaPdvRequestDTO dto, Long usuarioId) {
        String chave = dto.getChaveVenda().trim();
        VendaPdv existente = vendaPdvRepository.findByChaveVenda(chave).orElse(null);
        if (existente != null) {
            if (!existente.getPedido().getBarraca().getId().equals(dto.getBarracaId())) {
                throw new CadastroException(HttpStatus.CONFLICT, "Chave de venda já usada em outro ponto de venda.");
            }
            return montarResposta(existente);
        }

        Barraca barraca = buscarBarracaAutorizada(dto.getBarracaId(), usuarioId);
        String forma = validarForma(dto.getFormaPagamento());
        String documento = validarDocumento(dto.getClienteDocumento());
        Usuario operador = resolverOperador(usuarioId, barraca);

        // Ordena por produto para que vendas simultâneas travem as linhas na mesma ordem (sem deadlock).
        List<ItemPedido> itens = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> entrada : somarQuantidades(dto.getItens()).entrySet()) {
            ItemPedido item = baixarEstoque(barraca, entrada.getKey(), entrada.getValue());
            itens.add(item);
            total = total.add(item.getSubtotal());
        }

        BigDecimal recebido = null;
        BigDecimal troco = null;
        if ("dinheiro".equals(forma)) {
            recebido = dto.getValorRecebido() != null
                    ? dto.getValorRecebido().setScale(2, RoundingMode.HALF_UP)
                    : null;
            if (recebido == null || recebido.compareTo(total) < 0) {
                throw new CadastroException(HttpStatus.BAD_REQUEST, "Valor recebido menor que o total da venda.");
            }
            troco = recebido.subtract(total);
        }

        Pedido pedido = new Pedido();
        pedido.setTipo("pdv");
        pedido.setBarraca(barraca);
        pedido.setOperador(operador);
        pedido.setMetodoPagamento(forma);
        pedido.setValorTotal(total);
        pedido.setStatus(STATUS_CONCLUIDO);
        pedido = pedidoRepository.save(pedido);

        for (ItemPedido item : itens) {
            item.setPedido(pedido);
            itemPedidoRepository.save(item);
        }

        if (total.signum() > 0) {
            Pagamento pagamento = new Pagamento();
            pagamento.setPedido(pedido);
            pagamento.setMetodo(forma);
            pagamento.setValor(total);
            pagamento.setStatus("aprovado");
            pagamentoRepository.save(pagamento);
        }

        VendaPdv venda = new VendaPdv();
        venda.setPedido(pedido);
        venda.setChaveVenda(chave);
        venda.setValorRecebido(recebido);
        venda.setTroco(troco);
        venda.setClienteNome(textoOuNulo(dto.getClienteNome()));
        venda.setClienteDocumento(documento);
        // saveAndFlush: se outra requisição gravou a mesma chave, o erro aparece aqui e tudo é desfeito.
        venda = vendaPdvRepository.saveAndFlush(venda);

        return new VendaPdvResponseDTO(venda, itens);
    }

    /** Usada quando duas requisições com a mesma chave chegam juntas e a segunda perde no índice único. */
    @Transactional(readOnly = true)
    public VendaPdvResponseDTO buscarPorChave(String chaveVenda) {
        return vendaPdvRepository.findByChaveVenda(chaveVenda.trim())
                .map(this::montarResposta)
                .orElseThrow(() -> new CadastroException(HttpStatus.CONFLICT, "Não foi possível registrar a venda. Tente novamente."));
    }

    @Transactional(readOnly = true)
    public VendaPdvResponseDTO buscarVenda(Long pedidoId, Long usuarioId) {
        VendaPdv venda = vendaPdvRepository.findByPedidoId(pedidoId)
                .orElseThrow(() -> new CadastroException(HttpStatus.NOT_FOUND, "Venda não encontrada."));
        validarEmpresa(venda.getPedido().getBarraca(), usuarioId);
        return montarResposta(venda);
    }

    private VendaPdvResponseDTO montarResposta(VendaPdv venda) {
        return new VendaPdvResponseDTO(venda, itemPedidoRepository.findByPedidoId(venda.getPedido().getId()));
    }

    private ItemPedido baixarEstoque(Barraca barraca, Long produtoId, BigDecimal quantidade) {
        EstoqueBarraca estoque = estoqueBarracaRepository.findParaVenda(barraca.getId(), produtoId)
                .orElseThrow(() -> new CadastroException(HttpStatus.BAD_REQUEST,
                        "Produto não está disponível neste ponto de venda."));
        Produto produto = produtoRepository.findParaVenda(produtoId)
                .orElseThrow(() -> new CadastroException(HttpStatus.BAD_REQUEST, "Produto não encontrado."));

        if (!Integer.valueOf(1).equals(produto.getAtivo())) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "\"" + produto.getNome() + "\" está inativo.");
        }

        BigDecimal noQuiosque = valorOuZero(estoque.getQuantidade());
        BigDecimal noProduto = valorOuZero(produto.getEstoque());
        BigDecimal disponivel = noQuiosque.min(noProduto).max(BigDecimal.ZERO);
        if (disponivel.compareTo(quantidade) < 0) {
            throw new CadastroException(HttpStatus.BAD_REQUEST,
                    "Estoque insuficiente para \"" + produto.getNome() + "\". Disponível: "
                            + disponivel.stripTrailingZeros().toPlainString());
        }

        estoque.setQuantidade(noQuiosque.subtract(quantidade));
        produto.setEstoque(noProduto.subtract(quantidade));
        estoqueBarracaRepository.save(estoque);
        produtoRepository.save(produto);

        BigDecimal preco = valorOuZero(produto.getPrecoVenda()).setScale(2, RoundingMode.HALF_UP);
        ItemPedido item = new ItemPedido();
        item.setProduto(produto);
        item.setQuantidade(quantidade);
        item.setPrecoUnitario(preco);
        item.setSubtotal(preco.multiply(quantidade).setScale(2, RoundingMode.HALF_UP));
        return item;
    }

    /** Junta linhas repetidas do mesmo produto e ordena por id. */
    static Map<Long, BigDecimal> somarQuantidades(List<VendaPdvRequestDTO.Item> itens) {
        Map<Long, BigDecimal> somas = new LinkedHashMap<>();
        itens.stream()
                .sorted(Comparator.comparing(VendaPdvRequestDTO.Item::getProdutoId))
                .forEach(i -> somas.merge(i.getProdutoId(), i.getQuantidade().setScale(3, RoundingMode.HALF_UP), BigDecimal::add));
        return somas;
    }

    static String validarForma(String forma) {
        String valor = forma == null ? "" : forma.trim().toLowerCase(Locale.ROOT);
        if (!FORMAS_PAGAMENTO.contains(valor)) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Forma de pagamento inválida.");
        }
        return valor;
    }

    /** CPF (11 dígitos) ou CNPJ (14). Vazio é aceito: o documento é opcional. */
    static String validarDocumento(String documento) {
        if (documento == null) return null;
        String digitos = documento.replaceAll("\\D", "");
        if (digitos.isEmpty()) return null;
        if (digitos.length() != 11 && digitos.length() != 14) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "CPF ou CNPJ do cliente incompleto.");
        }
        return digitos;
    }

    private Barraca buscarBarracaAutorizada(Long barracaId, Long usuarioId) {
        if (barracaId == null) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Informe o ponto de venda.");
        }
        Barraca barraca = barracaRepository.findById(barracaId)
                .orElseThrow(() -> new CadastroException(HttpStatus.NOT_FOUND, "Ponto de venda não encontrado."));
        if (!Integer.valueOf(1).equals(barraca.getAtiva())) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Ponto de venda desativado.");
        }
        validarEmpresa(barraca, usuarioId);
        return barraca;
    }

    /** Sem login (autenticação desligada) não há como checar; com login, só a dona do quiosque. */
    private void validarEmpresa(Barraca barraca, Long usuarioId) {
        if (usuarioId == null) return;
        Long empresaUsuario = usuarioRepository.findById(usuarioId)
                .map(u -> u.getEmpresa() != null ? u.getEmpresa().getId() : null)
                .orElse(null);
        if (empresaUsuario == null || !empresaUsuario.equals(barraca.getEvento().getEmpresa().getId())) {
            throw new CadastroException(HttpStatus.FORBIDDEN, "Este ponto de venda não pertence à sua empresa.");
        }
    }

    /** O Pedido exige operador: o usuário logado ou, sem login, o responsável pelo quiosque. */
    private Usuario resolverOperador(Long usuarioId, Barraca barraca) {
        Usuario operador = usuarioId != null
                ? usuarioRepository.findById(usuarioId).orElse(barraca.getResponsavel())
                : barraca.getResponsavel();
        if (operador == null) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Ponto de venda sem responsável cadastrado.");
        }
        return operador;
    }

    private static boolean corresponde(Produto produto, String termo) {
        return contem(produto.getNome(), termo) || contem(produto.getCodigo(), termo);
    }

    private static boolean contem(String texto, String termo) {
        return texto != null && texto.toLowerCase(Locale.ROOT).contains(termo);
    }

    private static String textoOuNulo(String texto) {
        if (texto == null) return null;
        String limpo = texto.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    private static BigDecimal valorOuZero(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }
}
