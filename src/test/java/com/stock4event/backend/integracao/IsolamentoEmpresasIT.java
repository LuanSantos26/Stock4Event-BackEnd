package com.stock4event.backend.integracao;

import com.stock4event.backend.entity.*;
import com.stock4event.backend.support.IntegracaoBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Isolamento entre empresas: o empresaId enviado pelo app não dá acesso a outra empresa (403),
 * e registros de outra empresa não podem ser consultados, alterados ou excluídos (403, banco intacto).
 */
class IsolamentoEmpresasIT extends IntegracaoBase {

    private Conta a;   // usuário logado
    private Conta b;   // outra empresa (vítima)

    @BeforeEach
    void contas() {
        a = novoCliente();
        b = novaConta("AMBOS");
    }

    @Test
    void manipularEmpresaIdNosParametrosResponde403() throws Exception {
        Long outra = b.empresaId();
        List<String> urls = List.of(
                "/api/enderecos?empresaId=" + outra,
                "/api/cartoes-pagamento?empresaId=" + outra,
                "/api/chaves-pix?empresaId=" + outra,
                "/api/formas-pagamento?empresaId=" + outra,
                "/api/barracas?empresaId=" + outra,
                "/api/produtos?empresaId=" + outra,
                "/api/caminhoneiros?empresaId=" + outra,
                "/api/logistica?empresaId=" + outra,
                "/api/eventos?empresaId=" + outra,
                "/api/financeiro/resumo?empresaCompradoraId=" + outra,
                "/api/financeiro/stock-dia?empresaCompradoraId=" + outra,
                "/api/notificacoes?empresaCompradoraId=" + outra,
                "/api/solicitacoes-compra?empresaCompradoraId=" + outra,
                "/api/solicitacoes-compra/recebidas?empresaFornecedoraId=" + outra,
                "/api/marketplace/fornecedores?empresaCompradoraId=" + outra,
                "/api/usuarios/empresa/" + outra);
        for (String url : urls) {
            MvcResult r = chamar(auth(GET(url), a));
            assertThat(r.getResponse().getStatus()).as(url).isEqualTo(403);
            assertThat(corpo(r).path("erro").asText()).as(url).isNotBlank();
        }
    }

    @Test
    void semEmpresaIdOServidorUsaAEmpresaDoToken() throws Exception {
        novoEndereco(a.empresa());
        EnderecoEntrega deB = novoEndereco(b.empresa());
        MvcResult r = chamar(auth(GET("/api/enderecos"), a));
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(r)).hasSize(1);
        corpo(r).forEach(e -> assertThat(e.path("empresaId").asLong()).isEqualTo(a.empresaId()));
        assertThat(corpo(r).toString()).doesNotContain("\"id\":" + deB.getId() + ",");
    }

    @Test
    void manipularEmpresaIdNoCorpoResponde403ENaoGrava() throws Exception {
        long enderecosB = enderecoRepository.findByEmpresaIdOrderByPrincipalDescApelidoAsc(b.empresaId()).size();
        int s = status(comCorpo(auth(POST("/api/enderecos"), a), Map.of(
                "empresaId", b.empresaId(), "apelido", "Invasão", "logradouro", "Rua X", "numero", "1",
                "bairro", "Centro", "cidade", "Recife", "uf", "PE", "cep", "50000-000")));
        assertThat(s).isEqualTo(403);
        assertThat(enderecoRepository.findByEmpresaIdOrderByPrincipalDescApelidoAsc(b.empresaId())).hasSize((int) enderecosB);

        long produtosB = produtoRepository.findByEmpresaIdAndAtivo(b.empresaId(), 1).size();
        s = status(comCorpo(auth(POST("/api/produtos"), a), Map.of(
                "empresaId", b.empresaId(), "nome", "Produto plantado", "precoVenda", 1, "unidade", "UN")));
        assertThat(s).isEqualTo(403);
        assertThat(produtoRepository.findByEmpresaIdAndAtivo(b.empresaId(), 1)).hasSize((int) produtosB);

        s = status(comCorpo(auth(POST("/api/cartoes-pagamento"), a), Map.of(
                "empresaId", b.empresaId(), "tipo", "credito", "bandeira", "Visa",
                "ultimosDigitos", "1111", "validade", "12/30", "titular", "X")));
        assertThat(s).isEqualTo(403);
        assertThat(cartaoRepository.findByEmpresaIdOrderByIdDesc(b.empresaId())).isEmpty();
    }

    @Test
    void comprarEmNomeDeOutraEmpresaOuDeOutroUsuarioResponde403() throws Exception {
        Conta fornecedor = novoFornecedor();
        Produto p = novoProduto(fornecedor.empresa(), "10", "5.00");
        EnderecoEntrega endA = novoEndereco(a.empresa());
        EnderecoEntrega endB = novoEndereco(b.empresa());

        Map<String, Object> pedido = new LinkedHashMap<>();
        pedido.put("empresaCompradoraId", b.empresaId());
        pedido.put("empresaFornecedoraId", fornecedor.empresaId());
        pedido.put("metodoPagamento", "pix");
        pedido.put("enderecoEntregaId", endB.getId());
        pedido.put("itens", List.of(Map.of("produtoId", p.getId(), "quantidade", 1)));
        assertThat(status(comCorpo(auth(POST("/api/solicitacoes-compra"), a), pedido))).isEqualTo(403);

        pedido.put("empresaCompradoraId", a.empresaId());
        pedido.put("usuarioId", b.usuarioId());
        pedido.put("enderecoEntregaId", endA.getId());
        assertThat(status(comCorpo(auth(POST("/api/solicitacoes-compra"), a), pedido))).isEqualTo(403);

        pedido.remove("usuarioId");
        pedido.put("enderecoEntregaId", endB.getId()); // endereço de outra empresa
        assertThat(status(comCorpo(auth(POST("/api/solicitacoes-compra"), a), pedido))).isEqualTo(403);

        assertThat(produtoRepository.findById(p.getId()).orElseThrow().getEstoque()).isEqualByComparingTo("10");
        assertThat(solicitacaoRepository.findByEmpresaCompradoraIdOrderByCriadoEmDesc(a.empresaId())).isEmpty();
        assertThat(solicitacaoRepository.findByEmpresaCompradoraIdOrderByCriadoEmDesc(b.empresaId())).isEmpty();
    }

    @Test
    void quiosqueDeOutraEmpresaNaoPodeSerLidoAlteradoOuExcluido() throws Exception {
        Barraca deB = novaBarraca(b);
        Produto produtoA = novoProduto(a.empresa(), "5", "1.00");
        String url = "/api/barracas/" + deB.getId();

        assertThat(status(auth(GET(url), a))).isEqualTo(403);
        assertThat(status(comCorpo(auth(PUT(url), a), Map.of("nome", "Renomeada", "responsavelId", a.usuarioId())))).isEqualTo(403);
        assertThat(status(comCorpo(auth(PUT(url + "/estoque"), a),
                Map.of("itens", List.of(Map.of("produtoId", produtoA.getId(), "quantidade", 1)))))).isEqualTo(403);
        assertThat(status(auth(DELETE(url), a))).isEqualTo(403);

        Barraca depois = barracaRepository.findById(deB.getId()).orElseThrow();
        assertThat(depois.getNome()).isEqualTo(deB.getNome());
        assertThat(depois.getAtiva()).isEqualTo(1);
        assertThat(estoqueBarracaRepository.findByBarracaId(deB.getId())).isEmpty();
    }

    @Test
    void enderecoCartaoPixEFormaDeOutraEmpresaNaoPodemSerAlteradosOuExcluidos() throws Exception {
        EnderecoEntrega end = novoEndereco(b.empresa());
        CartaoPagamentoSalvo cartao = novoCartao(b.empresa());

        assertThat(status(comCorpo(auth(PUT("/api/enderecos/" + end.getId()), a), Map.of(
                "apelido", "Hack", "logradouro", "Rua", "numero", "1", "bairro", "B",
                "cidade", "C", "uf", "PE", "cep", "55000-000")))).isEqualTo(403);
        assertThat(status(auth(PATCH("/api/enderecos/" + end.getId() + "/principal"), a))).isEqualTo(403);
        assertThat(status(auth(DELETE("/api/enderecos/" + end.getId()), a))).isEqualTo(403);
        assertThat(status(auth(DELETE("/api/cartoes-pagamento/" + cartao.getId()), a))).isEqualTo(403);

        assertThat(enderecoRepository.findById(end.getId()).orElseThrow().getApelido()).isEqualTo(end.getApelido());
        assertThat(cartaoRepository.existsById(cartao.getId())).isTrue();
    }

    @Test
    void produtoDeOutraEmpresaNaoPodeSerAlteradoNemExcluido() throws Exception {
        Produto deB = novoProduto(b.empresa(), "8", "3.00");
        assertThat(status(comCorpo(auth(PUT("/api/produtos/" + deB.getId()), a), Map.of(
                "nome", "Alterado", "precoVenda", 0.01, "unidade", "UN")))).isEqualTo(403);
        assertThat(status(auth(DELETE("/api/produtos/" + deB.getId()), a))).isEqualTo(403);
        Produto depois = produtoRepository.findById(deB.getId()).orElseThrow();
        assertThat(depois.getNome()).isEqualTo(deB.getNome());
        assertThat(depois.getPrecoVenda()).isEqualByComparingTo("3.00");
        assertThat(depois.getAtivo()).isEqualTo(1);
    }

    @Test
    void produtoSoEhVisivelSeForDaEmpresaOuDoCatalogoDeFornecedor() throws Exception {
        Conta cliente = novoCliente();
        Produto estoqueInternoDoCliente = novoProduto(cliente.empresa(), "3", "9.00");
        assertThat(status(auth(GET("/api/produtos/" + estoqueInternoDoCliente.getId()), a))).isEqualTo(403);

        Produto catalogo = novoProduto(b.empresa(), "3", "9.00"); // b é AMBOS: aparece no marketplace
        assertThat(status(auth(GET("/api/produtos/" + catalogo.getId()), a))).isEqualTo(200);
        assertThat(status(auth(GET("/api/produtos/" + estoqueInternoDoCliente.getId()), cliente))).isEqualTo(200);
    }

    @Test
    void empresaEUsuariosDeOutraEmpresaSaoProtegidos() throws Exception {
        assertThat(status(auth(GET("/api/empresas/" + b.empresaId()), a))).isEqualTo(403);
        assertThat(status(comCorpo(auth(PUT("/api/empresas/" + b.empresaId()), a),
                Map.of("nome", "Tomada", "cnpj", b.empresa().getCnpj())))).isEqualTo(403);
        assertThat(empresaRepository.findById(b.empresaId()).orElseThrow().getNome()).isEqualTo(b.empresa().getNome());

        assertThat(status(auth(GET("/api/usuarios/" + b.usuarioId()), a))).isEqualTo(403);
        assertThat(status(comCorpo(auth(PUT("/api/usuarios/" + b.usuarioId()), a), Map.of("senha", "invasao123")))).isEqualTo(403);
        assertThat(status(auth(DELETE("/api/usuarios/" + b.usuarioId()), a))).isEqualTo(403);
        assertThat(usuarioRepository.findById(b.usuarioId()).orElseThrow().getAtivo()).isEqualTo(1);
        // senha de B continua a mesma
        assertThat(status(comCorpo(POST("/api/usuarios/login"), Map.of("email", b.usuario().getEmail(), "senha", SENHA)))).isEqualTo(200);
    }

    @Test
    void listagensGlobaisMostramSoAPropriaEmpresa() throws Exception {
        MvcResult usuarios = chamar(auth(GET("/api/usuarios"), a));
        corpo(usuarios).forEach(u -> assertThat(u.path("empresa").path("id").asLong()).isEqualTo(a.empresaId()));
        MvcResult empresas = chamar(auth(GET("/api/empresas"), a));
        assertThat(corpo(empresas)).hasSize(1);
        assertThat(corpo(empresas).get(0).path("id").asLong()).isEqualTo(a.empresaId());

        novaBarraca(b);
        assertThat(corpo(chamar(auth(GET("/api/barracas"), a)))).isEmpty();
        assertThat(corpo(chamar(auth(GET("/api/estoque-barraca"), a)))).isEmpty();
    }

    @Test
    void pedidoDeOutraEmpresaNaoPodeSerLidoNemAlterado() throws Exception {
        Barraca barracaB = novaBarraca(b);
        Pedido p = new Pedido();
        p.setTipo("pdv");
        p.setBarraca(barracaB);
        p.setOperador(b.usuario());
        p.setStatus("aberto");
        p.setValorTotal(BigDecimal.TEN);
        p = pedidoRepository.save(p);

        assertThat(status(auth(GET("/api/pedidos/" + p.getId()), a))).isEqualTo(403);
        assertThat(status(auth(GET("/api/pedidos/" + p.getId() + "/itens"), a))).isEqualTo(403);
        assertThat(status(comCorpo(auth(PATCH("/api/pedidos/" + p.getId() + "/status"), a), Map.of("status", "pago")))).isEqualTo(403);
        assertThat(status(auth(DELETE("/api/pedidos/" + p.getId()), a))).isEqualTo(403);
        assertThat(status(auth(GET("/api/pedidos?barracaId=" + barracaB.getId()), a))).isEqualTo(403);
        assertThat(corpo(chamar(auth(GET("/api/pedidos"), a)))).isEmpty();
        assertThat(pedidoRepository.findById(p.getId()).orElseThrow().getStatus()).isEqualTo("aberto");
    }

    @Test
    void solicitacaoDeCompraSoPodeSerVistaOuMudadaPelasPartes() throws Exception {
        Conta fornecedor = novoFornecedor();
        Produto p = novoProduto(fornecedor.empresa(), "10", "5.00");
        EnderecoEntrega end = novoEndereco(b.empresa());
        MvcResult criada = chamar(comCorpo(auth(POST("/api/solicitacoes-compra"), b), Map.of(
                "empresaFornecedoraId", fornecedor.empresaId(), "metodoPagamento", "pix",
                "enderecoEntregaId", end.getId(), "taxaEntrega", 7,
                "itens", List.of(Map.of("produtoId", p.getId(), "quantidade", 2)))));
        assertThat(criada.getResponse().getStatus()).isEqualTo(201);
        long id = corpo(criada).path("id").asLong();

        assertThat(status(auth(GET("/api/solicitacoes-compra/" + id), a))).isEqualTo(403);
        assertThat(status(auth(PATCH("/api/solicitacoes-compra/" + id + "/cancelar"), a))).isEqualTo(403);
        assertThat(status(comCorpo(auth(PATCH("/api/solicitacoes-compra/" + id + "/status"), a),
                Map.of("status", "em_rota")))).isEqualTo(403);
        // nem o comprador pode mudar o status como se fosse o fornecedor
        assertThat(status(comCorpo(auth(PATCH("/api/solicitacoes-compra/" + id + "/status"), b),
                Map.of("status", "entregue")))).isEqualTo(403);

        assertThat(solicitacaoRepository.findById(id).orElseThrow().getStatus()).isEqualTo("recebido");
    }

    @Test
    void operacoesSemAdministradorDaPlataformaSaoRecusadas() throws Exception {
        long empresas = empresaRepository.count();
        assertThat(status(comCorpo(auth(POST("/api/empresas"), a), Map.of("nome", "Nova", "cnpj", "11.111.111/0001-11")))).isEqualTo(403);
        assertThat(status(auth(DELETE("/api/empresas/" + a.empresaId()), a))).isEqualTo(403);
        assertThat(empresaRepository.count()).isEqualTo(empresas);

        long perfis = perfilRepository.count();
        assertThat(status(comCorpo(auth(POST("/api/perfis"), a), Map.of("nome", "SuperAdmin")))).isEqualTo(403);
        assertThat(status(auth(DELETE("/api/perfis/" + perfilAdmin().getId()), a))).isEqualTo(403);
        assertThat(perfilRepository.count()).isEqualTo(perfis);
    }
}
