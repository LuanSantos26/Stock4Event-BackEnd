package com.stock4event.backend.integracao;

import com.fasterxml.jackson.databind.JsonNode;
import com.stock4event.backend.entity.*;
import com.stock4event.backend.support.IntegracaoBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Usuários autorizados continuam cadastrando, consultando, editando e excluindo; dados gravados na empresa certa. */
class FluxosValidosIT extends IntegracaoBase {

    @Test
    void enderecoCrudCompleto() throws Exception {
        Conta c = novoCliente();
        MvcResult criado = chamar(comCorpo(auth(POST("/api/enderecos"), c), Map.of(
                "apelido", " Depósito ", "logradouro", "Rua Nova", "numero", "50", "bairro", "Indianópolis",
                "cidade", "Caruaru", "uf", "pe", "cep", "55026000")));
        assertThat(criado.getResponse().getStatus()).isEqualTo(201);
        long id = corpo(criado).path("id").asLong();

        EnderecoEntrega salvo = enderecoRepository.findById(id).orElseThrow();
        assertThat(salvo.getEmpresa().getId()).isEqualTo(c.empresaId());
        assertThat(salvo.getApelido()).isEqualTo("Depósito");
        assertThat(salvo.getUf()).isEqualTo("PE");
        assertThat(salvo.getCep()).isEqualTo("55026-000");
        assertThat(salvo.isPrincipal()).isTrue(); // primeiro endereço vira principal

        assertThat(status(comCorpo(auth(PUT("/api/enderecos/" + id), c), Map.of(
                "apelido", "Depósito 2", "logradouro", "Rua Nova", "numero", "51", "bairro", "Indianópolis",
                "cidade", "Caruaru", "uf", "PE", "cep", "55026-000")))).isEqualTo(200);
        assertThat(enderecoRepository.findById(id).orElseThrow().getNumero()).isEqualTo("51");

        JsonNode lista = corpo(chamar(auth(GET("/api/enderecos"), c)));
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).path("apelido").asText()).isEqualTo("Depósito 2");

        assertThat(status(auth(DELETE("/api/enderecos/" + id), c))).isEqualTo(204);
        assertThat(enderecoRepository.existsById(id)).isFalse();
    }

    @Test
    void produtoCrudCompleto() throws Exception {
        Conta f = novoFornecedor();
        MvcResult criado = chamar(comCorpo(auth(POST("/api/produtos"), f), Map.of(
                "nome", "Água Mineral 500ml", "precoVenda", 2.5, "unidade", "UN", "estoque", 40, "empresaId", f.empresaId())));
        assertThat(criado.getResponse().getStatus()).isEqualTo(201);
        long id = corpo(criado).path("id").asLong();
        Produto p = produtoRepository.findById(id).orElseThrow();
        assertThat(p.getEmpresa().getId()).isEqualTo(f.empresaId());
        assertThat(p.getCodigo()).startsWith("EMP-" + f.empresaId() + "-");
        assertThat(p.getEstoque()).isEqualByComparingTo("40");
        assertThat(p.getAtivo()).isEqualTo(1);

        assertThat(status(comCorpo(auth(PUT("/api/produtos/" + id), f), Map.of(
                "nome", "Água Mineral 510ml", "precoVenda", 2.75, "unidade", "UN", "estoque", 35)))).isEqualTo(200);
        JsonNode lido = corpo(chamar(auth(GET("/api/produtos/" + id), f)));
        assertThat(lido.path("nome").asText()).isEqualTo("Água Mineral 510ml");
        assertThat(lido.path("precoVenda").decimalValue()).isEqualByComparingTo("2.75");
        assertThat(lido.path("estoque").decimalValue()).isEqualByComparingTo("35");
        assertThat(lido.path("empresaId").asLong()).isEqualTo(f.empresaId());

        assertThat(corpo(chamar(auth(GET("/api/produtos"), f))).toString()).contains("Água Mineral 510ml");
        assertThat(status(auth(DELETE("/api/produtos/" + id), f))).isEqualTo(200);
        assertThat(produtoRepository.findById(id).orElseThrow().getAtivo()).isEqualTo(0);
    }

    @Test
    void quiosqueComEstoqueCrudCompleto() throws Exception {
        Conta c = novoCliente();
        Produto p1 = novoProduto(c.empresa(), "10", "4.00");
        Produto p2 = novoProduto(c.empresa(), "6", "8.00");
        MvcResult criado = chamar(comCorpo(auth(POST("/api/barracas"), c), Map.of(
                "nome", "Barraca São João", "empresaId", c.empresaId(), "responsavelId", c.usuarioId(),
                "itens", List.of(Map.of("produtoId", p1.getId(), "quantidade", 3), Map.of("produtoId", p2.getId(), "quantidade", 2)))));
        assertThat(criado.getResponse().getStatus()).isEqualTo(201);
        long id = corpo(criado).path("id").asLong();
        assertThat(corpo(criado).path("totalProdutos").asInt()).isEqualTo(2);
        assertThat(estoqueBarracaRepository.findByBarracaIdAndProdutoId(id, p1.getId()).orElseThrow().getQuantidade()).isEqualByComparingTo("3");
        assertThat(AcessoEmpresa.da(barracaRepository.findById(id).orElseThrow())).isEqualTo(c.empresaId());

        assertThat(status(comCorpo(auth(PUT("/api/barracas/" + id + "/estoque"), c),
                Map.of("itens", List.of(Map.of("produtoId", p1.getId(), "quantidade", 5)))))).isEqualTo(200);
        assertThat(estoqueBarracaRepository.findByBarracaIdAndProdutoId(id, p1.getId()).orElseThrow().getQuantidade()).isEqualByComparingTo("5");

        assertThat(status(comCorpo(auth(PUT("/api/barracas/" + id), c), Map.of("nome", "Barraca Renomeada", "responsavelId", c.usuarioId())))).isEqualTo(200);
        assertThat(corpo(chamar(auth(GET("/api/barracas/" + id), c))).path("nome").asText()).isEqualTo("Barraca Renomeada");

        assertThat(status(auth(DELETE("/api/barracas/" + id), c))).isEqualTo(204);
        assertThat(barracaRepository.findById(id).orElseThrow().getAtiva()).isEqualTo(0);
    }

    @Test
    void cartaoPixEFormaDePagamento() throws Exception {
        Conta c = novoCliente();
        MvcResult cartao = chamar(comCorpo(auth(POST("/api/cartoes-pagamento"), c), Map.of(
                "tipo", "Credito", "bandeira", "Mastercard", "ultimosDigitos", "9876", "validade", "08/29", "titular", "ANA")));
        assertThat(cartao.getResponse().getStatus()).isEqualTo(201);
        assertThat(corpo(cartao).path("numeroMascarado").asText()).endsWith("9876");
        assertThat(corpo(chamar(auth(GET("/api/cartoes-pagamento?tipo=credito"), c)))).hasSize(1);

        MvcResult pix = chamar(comCorpo(auth(POST("/api/chaves-pix"), c), Map.of(
                "apelido", "CNPJ", "tipoChave", "cnpj", "chaveMascarada", "••.•••.•••/••••-01", "ultimosDigitos", "01")));
        assertThat(pix.getResponse().getStatus()).isEqualTo(201);
        assertThat(chavePixRepository.findByEmpresaIdOrderByIdDesc(c.empresaId())).hasSize(1);

        MvcResult forma = chamar(comCorpo(auth(POST("/api/formas-pagamento"), c), Map.of("tipo", "dinheiro", "apelido", "Caixa")));
        assertThat(forma.getResponse().getStatus()).isEqualTo(201);
        assertThat(corpo(forma).path("principal").asBoolean()).isTrue();

        assertThat(status(auth(DELETE("/api/cartoes-pagamento/" + corpo(cartao).path("id").asLong()), c))).isEqualTo(204);
        assertThat(status(auth(DELETE("/api/chaves-pix/" + corpo(pix).path("id").asLong()), c))).isEqualTo(204);
        assertThat(cartaoRepository.findByEmpresaIdOrderByIdDesc(c.empresaId())).isEmpty();
    }

    @Test
    void compraCompletaComEntregaCreditaOEstoqueDoComprador() throws Exception {
        Conta cliente = novoCliente();
        Conta fornecedor = novoFornecedor();
        Produto produto = novoProduto(fornecedor.empresa(), "20", "5.50");
        EnderecoEntrega end = novoEndereco(cliente.empresa());

        MvcResult criada = chamar(comCorpo(auth(POST("/api/solicitacoes-compra"), cliente), Map.of(
                "empresaCompradoraId", cliente.empresaId(), "usuarioId", cliente.usuarioId(),
                "empresaFornecedoraId", fornecedor.empresaId(), "metodoPagamento", "PIX",
                "enderecoEntregaId", end.getId(), "taxaEntrega", 7,
                "itens", List.of(Map.of("produtoId", produto.getId(), "quantidade", 4)))));
        assertThat(criada.getResponse().getStatus()).isEqualTo(201);
        JsonNode s = corpo(criada);
        long id = s.path("id").asLong();
        assertThat(s.path("compradorId").asLong()).isEqualTo(cliente.empresaId());
        assertThat(s.path("valorTotal").decimalValue()).isEqualByComparingTo("29.00"); // 4 × 5,50 + 7
        assertThat(s.path("status").asText()).isEqualTo("recebido");
        assertThat(produtoRepository.findById(produto.getId()).orElseThrow().getEstoque()).isEqualByComparingTo("16");

        SolicitacaoCompra salva = solicitacaoRepository.findById(id).orElseThrow();
        assertThat(salva.getUsuarioSolicitante().getId()).isEqualTo(cliente.usuarioId());
        assertThat(pagamentoRepository.findByPedidoId(salva.getPedido().getId()).get(0).getStatus()).isEqualTo("aprovado");

        assertThat(corpo(chamar(auth(GET("/api/solicitacoes-compra/recebidas"), fornecedor)))).hasSize(1);

        // As 4 etapas são percorridas uma a uma; pular etapa é recusado.
        assertThat(status(comCorpo(auth(PATCH("/api/solicitacoes-compra/" + id + "/status"), fornecedor),
                Map.of("status", "entregue")))).isEqualTo(400);
        assertThat(status(comCorpo(auth(PATCH("/api/solicitacoes-compra/" + id + "/status"), fornecedor),
                Map.of("status", "preparando")))).isEqualTo(200);
        assertThat(status(comCorpo(auth(PATCH("/api/solicitacoes-compra/" + id + "/status"), fornecedor),
                Map.of("status", "em_rota")))).isEqualTo(200);
        assertThat(status(comCorpo(auth(PATCH("/api/solicitacoes-compra/" + id + "/status"), fornecedor),
                Map.of("empresaFornecedoraId", fornecedor.empresaId(), "status", "entregue")))).isEqualTo(200);

        Produto recebido = produtoRepository.findByEmpresaIdAndCodigoOrigem(cliente.empresaId(), produto.getCodigo()).orElseThrow();
        assertThat(recebido.getEstoque()).isEqualByComparingTo("4");
        assertThat(corpo(chamar(auth(GET("/api/solicitacoes-compra/" + id), cliente))).path("status").asText()).isEqualTo("entregue");
    }

    @Test
    void cancelamentoPeloCompradorDevolveEstoqueEEstornaPagamento() throws Exception {
        Conta cliente = novoCliente();
        Conta fornecedor = novoFornecedor();
        Produto produto = novoProduto(fornecedor.empresa(), "10", "2.00");
        EnderecoEntrega end = novoEndereco(cliente.empresa());
        long id = corpo(chamar(comCorpo(auth(POST("/api/solicitacoes-compra"), cliente), Map.of(
                "empresaFornecedoraId", fornecedor.empresaId(), "metodoPagamento", "credito",
                "enderecoEntregaId", end.getId(), "itens", List.of(Map.of("produtoId", produto.getId(), "quantidade", 3)))))).path("id").asLong();
        assertThat(produtoRepository.findById(produto.getId()).orElseThrow().getEstoque()).isEqualByComparingTo("7");

        assertThat(status(auth(PATCH("/api/solicitacoes-compra/" + id + "/cancelar"), cliente))).isEqualTo(200);
        assertThat(produtoRepository.findById(produto.getId()).orElseThrow().getEstoque()).isEqualByComparingTo("10");
        SolicitacaoCompra s = solicitacaoRepository.findById(id).orElseThrow();
        assertThat(s.getStatus()).isEqualTo("cancelada");
        assertThat(pagamentoRepository.findByPedidoId(s.getPedido().getId()).get(0).getStatus()).isEqualTo("estornado");
    }

    @Test
    void usuarioEEmpresaAtualizamOsPropriosDados() throws Exception {
        Conta c = novoCliente();
        String novoEmail = "renomeado" + seq() + "@teste.local";
        assertThat(status(comCorpo(auth(PUT("/api/usuarios/" + c.usuarioId()), c),
                Map.of("nome", "Nome Novo", "email", novoEmail, "senha", "outraSenha1")))).isEqualTo(200);
        Usuario u = usuarioRepository.findById(c.usuarioId()).orElseThrow();
        assertThat(u.getNome()).isEqualTo("Nome Novo");
        assertThat(u.getEmail()).isEqualTo(novoEmail);
        assertThat(status(comCorpo(POST("/api/usuarios/login"), Map.of("email", novoEmail, "senha", "outraSenha1")))).isEqualTo(200);

        assertThat(status(comCorpo(auth(PUT("/api/empresas/" + c.empresaId()), c),
                Map.of("nome", "Bar Renomeado", "cnpj", c.empresa().getCnpj(), "telefone", "(81) 98888-0000")))).isEqualTo(200);
        Empresa e = empresaRepository.findById(c.empresaId()).orElseThrow();
        assertThat(e.getNome()).isEqualTo("Bar Renomeado");
        assertThat(e.getTelefone()).isEqualTo("(81) 98888-0000");
    }

    @Test
    void usuarioCriaEDesativaColegaDaPropriaEmpresa() throws Exception {
        Conta c = novoCliente();
        String email = "colega" + seq() + "@teste.local";
        MvcResult r = chamar(comCorpo(auth(POST("/api/usuarios/register"), c),
                Map.of("nome", "Colega", "email", email, "senha", "senha123", "perfilId", perfilAdmin().getId())));
        assertThat(r.getResponse().getStatus()).isEqualTo(201);
        long colega = corpo(r).path("id").asLong();
        assertThat(usuarioRepository.findById(colega).orElseThrow().getEmpresa().getId()).isEqualTo(c.empresaId());
        assertThat(status(auth(DELETE("/api/usuarios/" + colega), c))).isEqualTo(204);
        assertThat(usuarioRepository.findById(colega).orElseThrow().getAtivo()).isEqualTo(0);
    }

    @Test
    void vendaPdvComItensEPagamento() throws Exception {
        Conta c = novoCliente();
        Barraca barraca = novaBarraca(c);
        Produto p = novoProduto(c.empresa(), "10", "3.00");

        MvcResult pedido = chamar(comCorpo(auth(POST("/api/pedidos"), c), Map.of("barraca", Map.of("id", barraca.getId()))));
        assertThat(pedido.getResponse().getStatus()).isEqualTo(201);
        long pedidoId = corpo(pedido).path("id").asLong();

        assertThat(status(comCorpo(auth(POST("/api/itens-pedido"), c), Map.of(
                "pedido", Map.of("id", pedidoId), "produto", Map.of("id", p.getId()), "quantidade", 2, "precoUnitario", 3.0)))).isEqualTo(201);
        assertThat(pedidoRepository.findById(pedidoId).orElseThrow().getValorTotal()).isEqualByComparingTo("6.00");

        MvcResult pg = chamar(comCorpo(auth(POST("/api/pagamentos"), c), Map.of("pedido", Map.of("id", pedidoId), "metodo", "dinheiro", "valor", 6)));
        assertThat(pg.getResponse().getStatus()).isEqualTo(201);
        assertThat(status(comCorpo(auth(PATCH("/api/pagamentos/" + corpo(pg).path("id").asLong() + "/status"), c), Map.of("status", "aprovado")))).isEqualTo(200);
        assertThat(status(comCorpo(auth(PATCH("/api/pedidos/" + pedidoId + "/status"), c), Map.of("status", "fechado")))).isEqualTo(200);
        assertThat(pedidoRepository.findById(pedidoId).orElseThrow().getStatus()).isEqualTo("fechado");
        assertThat(corpo(chamar(auth(GET("/api/pedidos"), c)))).hasSize(1);
    }

    @Test
    void eventoECaminhoneiroDaPropriaEmpresa() throws Exception {
        Conta c = novoFornecedor();
        MvcResult ev = chamar(comCorpo(auth(POST("/api/eventos"), c), Map.of(
                "nome", "Festival", "dataInicio", LocalDate.now().toString(), "dataFim", LocalDate.now().plusDays(2).toString())));
        assertThat(ev.getResponse().getStatus()).isEqualTo(201);
        long eventoId = corpo(ev).path("id").asLong();
        assertThat(eventoRepository.findById(eventoId).orElseThrow().getEmpresa().getId()).isEqualTo(c.empresaId());
        assertThat(eventoRepository.findById(eventoId).orElseThrow().getStatus()).isEqualTo("planejado");
        assertThat(status(auth(DELETE("/api/eventos/" + eventoId), c))).isEqualTo(204);

        MvcResult cam = chamar(comCorpo(auth(POST("/api/caminhoneiros"), c), Map.of("nome", "Rafael", "veiculo", "Truck", "rota", "Caruaru → Recife")));
        assertThat(cam.getResponse().getStatus()).isEqualTo(201);
        assertThat(corpo(chamar(auth(GET("/api/caminhoneiros"), c)))).hasSize(1);
    }

    @Test
    void consultasDaPropriaEmpresaFuncionam() throws Exception {
        Conta c = novoCliente();
        for (String url : List.of("/api/financeiro/resumo", "/api/financeiro/stock-dia", "/api/notificacoes",
                "/api/marketplace/fornecedores", "/api/solicitacoes-compra", "/api/eventos", "/api/perfis",
                "/api/usuarios", "/api/empresas/" + c.empresaId(), "/api/usuarios/" + c.usuarioId())) {
            assertThat(status(auth(GET(url), c))).as(url).isEqualTo(200);
        }
        // empresaId igual ao do token continua aceito (compatível com o app)
        assertThat(status(auth(GET("/api/financeiro/resumo?empresaCompradoraId=" + c.empresaId()), c))).isEqualTo(200);
    }

    /** Pequeno utilitário de leitura para os testes. */
    static final class AcessoEmpresa {
        static Long da(Barraca b) { return b.getEvento().getEmpresa().getId(); }
    }
}
