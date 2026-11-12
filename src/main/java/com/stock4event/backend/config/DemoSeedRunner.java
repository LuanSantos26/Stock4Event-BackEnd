package com.stock4event.backend.config;

import com.stock4event.backend.entity.*;
import com.stock4event.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Conta de apresentação (administradora, perfil cliente e fornecedor), com endereço,
 * cartão e PIX já cadastrados, para o pedido de demo ser reproduzível.
 * Idempotente: só cria o que faltar.
 * A empresa fica como CLIENTE para não aparecer como loja vazia no marketplace.
 * Roda antes do FormaPagamentoSeedRunner (@Order 91), que cria as formas de pagamento.
 */
@Component
@Order(80)
public class DemoSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoSeedRunner.class);

    public static final String DEMO_EMAIL = "demo@stock4event.com";
    public static final String DEMO_SENHA = "demo1234";
    private static final String DEMO_CNPJ = "12.345.678/0001-95";
    private static final String PERFIL_USO_DEMO = "ClienteFornecedor";

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PerfilRepository perfilRepository;
    private final EnderecoEntregaRepository enderecoRepository;
    private final CartaoPagamentoSalvoRepository cartaoRepository;
    private final ChavePixSalvaRepository chavePixRepository;

    public DemoSeedRunner(
            UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository,
            PerfilRepository perfilRepository,
            EnderecoEntregaRepository enderecoRepository,
            CartaoPagamentoSalvoRepository cartaoRepository,
            ChavePixSalvaRepository chavePixRepository) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.perfilRepository = perfilRepository;
        this.enderecoRepository = enderecoRepository;
        this.cartaoRepository = cartaoRepository;
        this.chavePixRepository = chavePixRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Empresa empresa = empresaRepository.findByCnpj(DEMO_CNPJ).orElseGet(this::criarEmpresa);

        Perfil perfilAdmin = perfilRepository.findByNomeIgnoreCase("Admin").orElse(null);
        if (perfilAdmin == null) {
            log.warn("Demo seed: perfil Admin não encontrado; conta demo não criada.");
            return;
        }

        // Administradora com acesso às telas de cliente e de fornecedor.
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(DEMO_EMAIL).orElse(null);
        if (usuario == null) {
            usuario = new Usuario();
            usuario.setNome("Conta Demo");
            usuario.setEmail(DEMO_EMAIL);
            usuario.setSenhaHash(BCrypt.hashpw(DEMO_SENHA, BCrypt.gensalt()));
            usuario.setEmpresa(empresa);
            usuario.setAtivo(1);
            log.info("Demo seed: conta {} criada.", DEMO_EMAIL);
        }
        usuario.setPerfil(perfilAdmin);
        usuario.setPerfilUso(PERFIL_USO_DEMO);
        usuarioRepository.save(usuario);

        if (enderecoRepository.countByEmpresaId(empresa.getId()) == 0) {
            EnderecoEntrega endereco = new EnderecoEntrega();
            endereco.setEmpresa(empresa);
            endereco.setApelido("Loja");
            endereco.setLogradouro("Av. Agamenon Magalhães");
            endereco.setNumero("100");
            endereco.setComplemento("Loja 1");
            endereco.setBairro("Maurício de Nassau");
            endereco.setCidade("Caruaru");
            endereco.setUf("PE");
            endereco.setCep("55012-290");
            endereco.setPrincipal(true);
            enderecoRepository.save(endereco);
        }

        if (cartaoRepository.findByEmpresaIdOrderByIdDesc(empresa.getId()).isEmpty()) {
            CartaoPagamentoSalvo cartao = new CartaoPagamentoSalvo();
            cartao.setEmpresa(empresa);
            cartao.setTipo("credito");
            cartao.setApelido("Cartão da loja");
            cartao.setBandeira("Visa");
            cartao.setUltimosDigitos("4242");
            cartao.setNumeroMascarado("•••• •••• •••• 4242");
            cartao.setValidade("12/30");
            cartao.setTitular("CONTA DEMO");
            cartaoRepository.save(cartao);
        }

        if (chavePixRepository.countByEmpresaId(empresa.getId()) == 0) {
            ChavePixSalva pix = new ChavePixSalva();
            pix.setEmpresa(empresa);
            pix.setApelido("CNPJ da empresa");
            pix.setTipoChave("cnpj");
            pix.setChaveMascarada("••.•••.•••/••••-95");
            pix.setUltimosDigitos("95");
            chavePixRepository.save(pix);
        }
    }

    private Empresa criarEmpresa() {
        Empresa empresa = new Empresa();
        empresa.setNome("Mercado Demo");
        empresa.setCnpj(DEMO_CNPJ);
        empresa.setTelefone("(81) 99999-0000");
        empresa.setTipo("CLIENTE");
        return empresaRepository.save(empresa);
    }
}
