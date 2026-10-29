package com.stock4event.backend.integracao;

import com.fasterxml.jackson.databind.JsonNode;
import com.stock4event.backend.entity.*;
import com.stock4event.backend.support.IntegracaoBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Campos controlados pelo servidor não podem ser alterados pelo cliente, e operações recusadas
 * (inclusive no meio de uma transação) não deixam gravações parciais.
 */
class ProtecaoCamposEOperacoesRejeitadasIT extends IntegracaoBase {

    // ── Proteção de campos internos ───────────────────────────────────────

    @Test
    void usuarioNaoAlteraPerfilEmpresaNemStatusPeloPut() throws Exception {
        Conta c = novoCliente();
        Conta outra = novoFornecedor();
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("nome", "Nome Ok");
        corpo.put("perfilId", 999);
        corpo.put("ativo", 0);
        corpo.put("empresa", Map.of("id", outra.empresaId()));
        corpo.put("empresaId", outra.empresaId());
        corpo.put("perfilUso", "Fornecedor");
        assertThat(status(comCorpo(auth(PUT("/api/usuarios/" + c.usuarioId()), c), corpo))).isEqualTo(200);

        Usuario u = usuarioRepository.findById(c.usuarioId()).orElseThrow();
        assertThat(u.getNome()).isEqualTo("Nome Ok");
        assertThat(u.getPerfil().getId()).isEqualTo(c.usuario().getPerfil().getId());
        assertThat(u.getEmpresa().getId()).isEqualTo(c.empresaId());
        assertThat(u.getAtivo()).isEqualTo(1);
        assertThat(u.getPerfilUso()).isEqualTo(c.usuario().getPerfilUso());
    }

    @Test
    void empresaNaoAlteraTipoLogoNemCapa() throws Exception {
        Conta c = novoCliente();
        assertThat(status(comCorpo(auth(PUT("/api/empresas/" + c.empresaId()), c), Map.of(
                "nome", "Bar", "cnpj", c.empresa().getCnpj(), "tipo", "DISTRIBUIDOR",
                "logoUrl", "https://malicioso.example/logo.png", "id", 1)))).isEqualTo(200);
        Empresa e = empresaRepository.findById(c.empresaId()).orElseThrow();
        assertThat(e.getTipo()).isEqualTo("CLIENTE");
        assertThat(e.getLogoUrl()).isNull();
    }

    @Test
    void fornecedorAtualizaPerfilDaLojaComImagensEnviadas() throws Exception {
        Conta c = novoFornecedor();
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("nome", "Loja");
        corpo.put("cnpj", c.empresa().getCnpj());
        corpo.put("descricao", "  Bebidas geladas para eventos  ");
        corpo.put("logoUrl", "/uploads/empresas/logo.jpg");
        corpo.put("capaUrl", "/uploads/empresas/capa.jpg");
        assertThat(status(comCorpo(auth(PUT("/api/empresas/" + c.empresaId()), c), corpo))).isEqualTo(200);
        Empresa e = empresaRepository.findById(c.empresaId()).orElseThrow();
        assertThat(e.getDescricao()).isEqualTo("Bebidas geladas para eventos");
        assertThat(e.getLogoUrl()).isEqualTo("/uploads/empresas/logo.jpg");
        assertThat(e.getCapaUrl()).isEqualTo("/uploads/empresas/capa.jpg");

        // Sem os campos: mantém. Texto vazio: remove.
        assertThat(status(comCorpo(auth(PUT("/api/empresas/" + c.empresaId()), c), Map.of(
                "nome", "Loja", "cnpj", c.empresa().getCnpj())))).isEqualTo(200);
        assertThat(empresaRepository.findById(c.empresaId()).orElseThrow().getLogoUrl())
                .isEqualTo("/uploads/empresas/logo.jpg");
        assertThat(status(comCorpo(auth(PUT("/api/empresas/" + c.empresaId()), c), Map.of(
                "nome", "Loja", "cnpj", c.empresa().getCnpj(), "capaUrl", "", "descricao", "")))).isEqualTo(200);
        e = empresaRepository.findById(c.empresaId()).orElseThrow();
        assertThat(e.getCapaUrl()).isNull();
        assertThat(e.getDescricao()).isNull();
        assertThat(e.getLogoUrl()).isEqualTo("/uploads/empresas/logo.jpg");
    }

    @Test
    void usuarioDefineFotoEnviadaMasIgnoraUrlExterna() throws Exception {
        Conta c = novoCliente();
        assertThat(status(comCorpo(auth(PUT("/api/usuarios/" + c.usuarioId()), c), Map.of(
                "fotoUrl", "https://malicioso.example/foto.png")))).isEqualTo(200);
        assertThat(usuarioRepository.findById(c.usuarioId()).orElseThrow().getFotoUrl()).isNull();

        assertThat(status(comCorpo(auth(PUT("/api/usuarios/" + c.usuarioId()), c), Map.of(
                "fotoUrl", "/uploads/usuarios/foto.jpg")))).isEqualTo(200);
        assertThat(usuarioRepository.findById(c.usuarioId()).orElseThrow().getFotoUrl())
                .isEqualTo("/uploads/usuarios/foto.jpg");

        assertThat(status(comCorpo(auth(PUT("/api/usuarios/" + c.usuarioId()), c), Map.of(
                "fotoUrl", "")))).isEqualTo(200);
        assertThat(usuarioRepository.findById(c.usuarioId()).orElseThrow().getFotoUrl()).isNull();
    }

    @Test
    void produtoNovoIgnoraIdCodigoEStatusEnviadosPeloCliente() throws Exception {
        Conta c = novoFornecedor();
        Conta outra = novoFornecedor();
        Produto alheio = novoProduto(outra.empresa(), "5", "10.00");
        MvcResult r = chamar(comCorpo(auth(POST("/api/produtos"), c), Map.of(
                "id", alheio.getId(), "codigo", alheio.getCodigo(), "ativo", 0,
                "nome", "Produto legítimo", "precoVenda", 1, "unidade", "UN")));
        assertThat(r.getResponse().getStatus()).isEqualTo(201);
        JsonNode novo = corpo(r);
        assertThat(novo.path("id").asLong()).isNotEqualTo(alheio.getId());
        assertThat(novo.path("codigo").asText()).isNotEqualTo(alheio.getCodigo());
        assertThat(novo.path("ativo").asInt()).isEqualTo(1);
        assertThat(novo.path("empresaId").asLong()).isEqualTo(c.empresaId());

        Produto intacto = produtoRepository.findById(alheio.getId()).orElseThrow();
        assertThat(intacto.getNome()).isEqualTo(alheio.getNome());
        assertThat(intacto.getEmpresa().getId()).isEqualTo(outra.empresaId());
    }

    @Test
    void pedidoPdvTemOperadorTipoStatusETotalDefinidosPeloServidor() throws Exception {
        Conta c = novoCliente();
        Conta outra = novoFornecedor();
        Barraca barraca = novaBarraca(c);
        Barraca barracaOutra = novaBarraca(outra);
        Pedido alheio = new Pedido();
        alheio.setTipo("pdv"); alheio.setBarraca(barracaOutra); alheio.setOperador(outra.usuario());
        alheio.setStatus("aberto"); alheio.setValorTotal(new BigDecimal("50"));
        alheio = pedidoRepository.save(alheio);

        Map<String, Object> corpo = new HashMap<>();
        corpo.put("id", alheio.getId());                      // tentativa de sobrescrever pedido alheio
        corpo.put("barraca", Map.of("id", barraca.getId()));
        corpo.put("operador", Map.of("id", outra.usuarioId()));
        corpo.put("empresaCompradora", Map.of("id", outra.empresaId()));
        corpo.put("tipo", "marketplace");
        corpo.put("status", "pago");
        corpo.put("valorTotal", 9999);
        MvcResult r = chamar(comCorpo(auth(POST("/api/pedidos"), c), corpo));
        assertThat(r.getResponse().getStatus()).isEqualTo(201);

        Pedido criado = pedidoRepository.findById(corpo(r).path("id").asLong()).orElseThrow();
        assertThat(criado.getId()).isNotEqualTo(alheio.getId());
        assertThat(criado.getOperador().getId()).isEqualTo(c.usuarioId());
        assertThat(criado.getEmpresaCompradora()).isNull();
        assertThat(criado.getTipo()).isEqualTo("pdv");
        assertThat(criado.getStatus()).isEqualTo("aberto");
        assertThat(criado.getValorTotal()).isEqualByComparingTo("0");

        Pedido intacto = pedidoRepository.findById(alheio.getId()).orElseThrow();
        assertThat(intacto.getOperador().getId()).isEqualTo(outra.usuarioId());
        assertThat(intacto.getValorTotal()).isEqualByComparingTo("50");

        assertThat(status(comCorpo(auth(PUT("/api/pedidos/" + criado.getId()), c), Map.of("status", "fechado", "valorTotal", 1234)))).isEqualTo(200);
        assertThat(pedidoRepository.findById(criado.getId()).orElseThrow().getValorTotal()).isEqualByComparingTo("0");
    }

    @Test
    void pagamentoNovoSempreComecaPendente() throws Exception {
        Conta c = novoCliente();
        Barraca barraca = novaBarraca(c);
        long pedidoId = corpo(chamar(comCorpo(auth(POST("/api/pedidos"), c), Map.of("barraca", Map.of("id", barraca.getId()))))).path("id").asLong();
        MvcResult r = chamar(comCorpo(auth(POST("/api/pagamentos"), c),
                Map.of("pedido", Map.of("id", pedidoId), "metodo", "pix", "valor", 10, "status", "aprovado")));
        assertThat(r.getResponse().getStatus()).isEqualTo(201);
        assertThat(pagamentoRepository.findById(corpo(r).path("id").asLong()).orElseThrow().getStatus()).isEqualTo("pendente");
    }

    @Test
    void eventoNaoPodeSerCriadoParaOutraEmpresa() throws Exception {
        Conta c = novoCliente();
        Conta outra = novoFornecedor();
        long antes = eventoRepository.findByEmpresaId(outra.empresaId()).size();
        assertThat(status(comCorpo(auth(POST("/api/eventos"), c), Map.of("empresa", Map.of("id", outra.empresaId()),
                "nome", "Invasão", "dataInicio", LocalDate.now().toString(), "dataFim", LocalDate.now().toString())))).isEqualTo(403);
        assertThat(eventoRepository.findByEmpresaId(outra.empresaId())).hasSize((int) antes);
    }

    @Test
    void solicitacaoIgnoraTotalEStatusEnviadosPeloCliente() throws Exception {
        Conta cliente = novoCliente();
        Conta fornecedor = novoFornecedor();
        Produto p = novoProduto(fornecedor.empresa(), "10", "10.00");
        EnderecoEntrega end = novoEndereco(cliente.empresa());
        MvcResult r = chamar(comCorpo(auth(POST("/api/solicitacoes-compra"), cliente), Map.of(
                "empresaFornecedoraId", fornecedor.empresaId(), "metodoPagamento", "dinheiro",
                "enderecoEntregaId", end.getId(), "valorTotal", 0.01, "status", "entregue",
                "itens", List.of(Map.of("produtoId", p.getId(), "quantidade", 2, "precoUnitario", 0.01)))));
        assertThat(r.getResponse().getStatus()).isEqualTo(201);
        assertThat(corpo(r).path("valorTotal").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(corpo(r).path("status").asText()).isEqualTo("recebido");
        assertThat(corpo(r).path("itens").get(0).path("precoUnitario").decimalValue()).isEqualByComparingTo("10.00");
    }

    @Test
    void pedidoDoMarketplaceNaoMudaPorForaDoFluxoDeSolicitacao() throws Exception {
        Conta cliente = novoCliente();
        Conta fornecedor = novoFornecedor();
        Produto p = novoProduto(fornecedor.empresa(), "10", "1.00");
        EnderecoEntrega end = novoEndereco(cliente.empresa());
        long solicitacaoId = corpo(chamar(comCorpo(auth(POST("/api/solicitacoes-compra"), cliente), Map.of(
                "empresaFornecedoraId", fornecedor.empresaId(), "metodoPagamento", "pix", "enderecoEntregaId", end.getId(),
                "itens", List.of(Map.of("produtoId", p.getId(), "quantidade", 1)))))).path("id").asLong();
        Pedido pedido = solicitacaoRepository.findById(solicitacaoId).orElseThrow().getPedido();

        assertThat(status(auth(GET("/api/pedidos/" + pedido.getId()), cliente))).isEqualTo(200);
        assertThat(status(comCorpo(auth(PATCH("/api/pedidos/" + pedido.getId() + "/status"), fornecedor), Map.of("status", "entregue")))).isEqualTo(409);
        assertThat(status(auth(DELETE("/api/pedidos/" + pedido.getId()), cliente))).isEqualTo(409);
        Long pagamentoId = pagamentoRepository.findByPedidoId(pedido.getId()).get(0).getId();
        assertThat(status(comCorpo(auth(PATCH("/api/pagamentos/" + pagamentoId + "/status"), cliente), Map.of("status", "estornado")))).isEqualTo(409);

        assertThat(pedidoRepository.findById(pedido.getId()).orElseThrow().getStatus()).isEqualTo("recebido");
        assertThat(pagamentoRepository.findById(pagamentoId).orElseThrow().getStatus()).isEqualTo("aprovado");
    }

    // ── Operações rejeitadas não deixam gravações parciais ─────────────────

    @Test
    void pedidoComUmItemSemEstoqueNaoDebitaNemGravaNada() throws Exception {
        Conta cliente = novoCliente();
        Conta fornecedor = novoFornecedor();
        Produto comEstoque = novoProduto(fornecedor.empresa(), "10", "1.00");
        Produto semEstoque = novoProduto(fornecedor.empresa(), "1", "1.00");
        EnderecoEntrega end = novoEndereco(cliente.empresa());
        long pedidos = pedidoRepository.count();
        long pagamentos = pagamentoRepository.count();

        int s = status(comCorpo(auth(POST("/api/solicitacoes-compra"), cliente), Map.of(
                "empresaFornecedoraId", fornecedor.empresaId(), "metodoPagamento", "pix", "enderecoEntregaId", end.getId(),
                "itens", List.of(Map.of("produtoId", comEstoque.getId(), "quantidade", 5),
                        Map.of("produtoId", semEstoque.getId(), "quantidade", 2)))));
        assertThat(s).isEqualTo(400);

        assertThat(produtoRepository.findById(comEstoque.getId()).orElseThrow().getEstoque()).isEqualByComparingTo("10");
        assertThat(produtoRepository.findById(semEstoque.getId()).orElseThrow().getEstoque()).isEqualByComparingTo("1");
        assertThat(solicitacaoRepository.findByEmpresaCompradoraIdOrderByCriadoEmDesc(cliente.empresaId())).isEmpty();
        assertThat(pedidoRepository.count()).isEqualTo(pedidos);
        assertThat(pagamentoRepository.count()).isEqualTo(pagamentos);
    }

    @Test
    void quiosqueComItemInvalidoNoMeioNaoFicaGravadoPelaMetade() throws Exception {
        Conta c = novoCliente();
        Conta outra = novoFornecedor();
        Produto meu = novoProduto(c.empresa(), "5", "1.00");
        Produto alheio = novoProduto(outra.empresa(), "5", "1.00");
        long barracasAntes = barracaRepository.findByEvento_EmpresaIdAndAtiva(c.empresaId(), 1).size();
        long estoqueAntes = estoqueBarracaRepository.count();

        int s = status(comCorpo(auth(POST("/api/barracas"), c), Map.of("nome", "Pela metade", "responsavelId", c.usuarioId(),
                "itens", List.of(Map.of("produtoId", meu.getId(), "quantidade", 2), Map.of("produtoId", alheio.getId(), "quantidade", 1)))));
        assertThat(s).isEqualTo(403);
        assertThat(barracaRepository.findByEvento_EmpresaIdAndAtiva(c.empresaId(), 1)).hasSize((int) barracasAntes);
        assertThat(estoqueBarracaRepository.count()).isEqualTo(estoqueAntes);
    }

    @Test
    void cadastroComCnpjDuplicadoNaoCriaUsuarioSolto() throws Exception {
        Conta existente = novoCliente();
        String email = "orfao" + seq() + "@teste.local";
        int s = status(comCorpo(POST("/api/cadastro"), Map.of(
                "empresa", Map.of("nome", "Cópia", "cnpj", existente.empresa().getCnpj()),
                "usuario", Map.of("nome", "Órfão", "email", email, "senha", "senha123"))));
        assertThat(s).isEqualTo(409);
        assertThat(usuarioRepository.existsByEmailIgnoreCase(email)).isFalse();
    }
}
