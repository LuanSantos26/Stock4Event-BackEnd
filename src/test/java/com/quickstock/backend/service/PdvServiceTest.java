package com.quickstock.backend.service;

import com.quickstock.backend.dto.VendaPdvRequestDTO;
import com.quickstock.backend.dto.VendaPdvResponseDTO;
import com.quickstock.backend.entity.Barraca;
import com.quickstock.backend.entity.Empresa;
import com.quickstock.backend.entity.EstoqueBarraca;
import com.quickstock.backend.entity.Evento;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PdvServiceTest {

    private BarracaRepository barracaRepository;
    private EstoqueBarracaRepository estoqueRepository;
    private ProdutoRepository produtoRepository;
    private UsuarioRepository usuarioRepository;
    private VendaPdvRepository vendaRepository;
    private PedidoRepository pedidoRepository;
    private ItemPedidoRepository itemPedidoRepository;
    private PagamentoRepository pagamentoRepository;
    private PdvService service;

    private Empresa empresa;
    private Barraca barraca;
    private Produto produto;
    private EstoqueBarraca estoque;
    private Usuario operador;

    @BeforeEach
    void setUp() {
        barracaRepository = mock(BarracaRepository.class);
        estoqueRepository = mock(EstoqueBarracaRepository.class);
        produtoRepository = mock(ProdutoRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        vendaRepository = mock(VendaPdvRepository.class);
        pedidoRepository = mock(PedidoRepository.class);
        itemPedidoRepository = mock(ItemPedidoRepository.class);
        pagamentoRepository = mock(PagamentoRepository.class);

        service = new PdvService();
        ReflectionTestUtils.setField(service, "barracaRepository", barracaRepository);
        ReflectionTestUtils.setField(service, "estoqueBarracaRepository", estoqueRepository);
        ReflectionTestUtils.setField(service, "produtoRepository", produtoRepository);
        ReflectionTestUtils.setField(service, "usuarioRepository", usuarioRepository);
        ReflectionTestUtils.setField(service, "vendaPdvRepository", vendaRepository);
        ReflectionTestUtils.setField(service, "pedidoRepository", pedidoRepository);
        ReflectionTestUtils.setField(service, "itemPedidoRepository", itemPedidoRepository);
        ReflectionTestUtils.setField(service, "pagamentoRepository", pagamentoRepository);

        empresa = new Empresa();
        empresa.setId(1L);
        empresa.setNome("Casa da Feira");
        empresa.setCnpj("12345678000199");

        Evento evento = new Evento();
        evento.setId(5L);
        evento.setEmpresa(empresa);

        operador = new Usuario();
        operador.setId(42L);
        operador.setNome("Ana");
        operador.setEmpresa(empresa);

        barraca = new Barraca();
        barraca.setId(10L);
        barraca.setNome("Quiosque 1");
        barraca.setEvento(evento);
        barraca.setResponsavel(operador);
        barraca.setAtiva(1);

        produto = new Produto();
        produto.setId(100L);
        produto.setEmpresa(empresa);
        produto.setNome("Heineken Long Neck");
        produto.setCodigo("EMP-1-0001");
        produto.setUnidade("un");
        produto.setPrecoVenda(new BigDecimal("8.50"));
        produto.setEstoque(new BigDecimal("20"));
        produto.setAtivo(1);

        estoque = new EstoqueBarraca();
        estoque.setId(7L);
        estoque.setBarraca(barraca);
        estoque.setProduto(produto);
        estoque.setQuantidade(new BigDecimal("5"));

        when(barracaRepository.findById(10L)).thenReturn(Optional.of(barraca));
        when(usuarioRepository.findById(42L)).thenReturn(Optional.of(operador));
        when(estoqueRepository.findParaVenda(10L, 100L)).thenReturn(Optional.of(estoque));
        when(produtoRepository.findParaVenda(100L)).thenReturn(Optional.of(produto));
        when(vendaRepository.findByChaveVenda(any())).thenReturn(Optional.empty());
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(3L);
            return p;
        });
        when(vendaRepository.saveAndFlush(any(VendaPdv.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private VendaPdvRequestDTO pedido(String forma, String quantidade) {
        VendaPdvRequestDTO.Item item = new VendaPdvRequestDTO.Item();
        item.setProdutoId(100L);
        item.setQuantidade(new BigDecimal(quantidade));
        VendaPdvRequestDTO dto = new VendaPdvRequestDTO();
        dto.setBarracaId(10L);
        dto.setChaveVenda("chave-1");
        dto.setFormaPagamento(forma);
        dto.setItens(List.of(item));
        return dto;
    }

    @Test
    void vendaEmDinheiroCalculaTotalTrocoEDaBaixaNosDoisEstoques() {
        VendaPdvRequestDTO dto = pedido("dinheiro", "2");
        dto.setValorRecebido(new BigDecimal("20"));

        VendaPdvResponseDTO venda = service.registrarVenda(dto, 42L);

        assertEquals("PDV-000003", venda.getNumero());
        assertEquals(0, new BigDecimal("17.00").compareTo(venda.getTotal()));
        assertEquals(0, new BigDecimal("3.00").compareTo(venda.getTroco()));
        assertEquals("Ana", venda.getOperadorNome());
        assertEquals("12345678000199", venda.getEmpresaCnpj());
        assertEquals(1, venda.getItens().size());
        assertEquals(0, new BigDecimal("3").compareTo(estoque.getQuantidade()));
        assertEquals(0, new BigDecimal("18").compareTo(produto.getEstoque()));

        ArgumentCaptor<Pedido> pedido = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(pedido.capture());
        assertEquals("pdv", pedido.getValue().getTipo());
        assertEquals("dinheiro", pedido.getValue().getMetodoPagamento());
        verify(itemPedidoRepository, times(1)).save(any());
        verify(pagamentoRepository).save(any(Pagamento.class));
    }

    @Test
    void mesmaChaveDevolveVendaJaRegistradaSemBaixarDeNovo() {
        Pedido pedidoAnterior = new Pedido();
        pedidoAnterior.setId(3L);
        pedidoAnterior.setBarraca(barraca);
        pedidoAnterior.setOperador(operador);
        pedidoAnterior.setMetodoPagamento("pix");
        pedidoAnterior.setValorTotal(new BigDecimal("8.50"));
        VendaPdv existente = new VendaPdv();
        existente.setId(1L);
        existente.setPedido(pedidoAnterior);
        existente.setChaveVenda("chave-1");
        when(vendaRepository.findByChaveVenda("chave-1")).thenReturn(Optional.of(existente));
        when(itemPedidoRepository.findByPedidoId(3L)).thenReturn(List.of());

        VendaPdvResponseDTO venda = service.registrarVenda(pedido("pix", "1"), 42L);

        assertEquals("PDV-000003", venda.getNumero());
        assertEquals(0, new BigDecimal("5").compareTo(estoque.getQuantidade()));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void recusaQuantidadeAcimaDoEstoqueDoQuiosque() {
        CadastroException erro = assertThrows(CadastroException.class,
                () -> service.registrarVenda(pedido("pix", "6"), 42L));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatus());
        assertTrue(erro.getMessage().contains("Estoque insuficiente"));
    }

    @Test
    void recusaDinheiroComValorRecebidoMenorQueOTotal() {
        VendaPdvRequestDTO dto = pedido("dinheiro", "2");
        dto.setValorRecebido(new BigDecimal("10"));

        assertThrows(CadastroException.class, () -> service.registrarVenda(dto, 42L));
    }

    @Test
    void recusaQuiosqueDeOutraEmpresa() {
        Empresa outra = new Empresa();
        outra.setId(2L);
        Usuario intruso = new Usuario();
        intruso.setId(99L);
        intruso.setEmpresa(outra);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(intruso));

        CadastroException erro = assertThrows(CadastroException.class,
                () -> service.registrarVenda(pedido("pix", "1"), 99L));

        assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
    }

    @Test
    void validaFormaEDocumento() {
        assertEquals("credito", PdvService.validarForma(" Credito "));
        assertThrows(CadastroException.class, () -> PdvService.validarForma("boleto"));
        assertNull(PdvService.validarDocumento("  "));
        assertEquals("12345678901", PdvService.validarDocumento("123.456.789-01"));
        assertThrows(CadastroException.class, () -> PdvService.validarDocumento("1234"));
    }
}
