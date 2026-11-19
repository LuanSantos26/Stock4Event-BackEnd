package com.stock4event.backend.config;

import com.stock4event.backend.entity.Empresa;
import com.stock4event.backend.entity.EnderecoEntrega;
import com.stock4event.backend.entity.Perfil;
import com.stock4event.backend.entity.Produto;
import com.stock4event.backend.entity.Usuario;
import com.stock4event.backend.repository.EmpresaRepository;
import com.stock4event.backend.repository.EnderecoEntregaRepository;
import com.stock4event.backend.repository.PerfilRepository;
import com.stock4event.backend.repository.ProdutoRepository;
import com.stock4event.backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Distribuidoras de bebidas (águas, refrigerantes, sucos e vinhos) e o catálogo delas,
 * com preço por unidade e por palete fechado.
 * Idempotente: empresas pelo CNPJ, produtos pelo código. Só cria o que faltar e
 * atualiza nome, preço, descrição e imagem; o estoque existente é mantido.
 * Roda antes do DadosBancariosDemoRunner (@Order 95), que cadastra o Pix das distribuidoras,
 * e antes do CodigoBarrasFixRunner, que gera os códigos de barras.
 */
@Component
@Order(90)
public class CatalogoBebidasSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogoBebidasSeedRunner.class);

    /** Senha das contas de fornecedor criadas aqui (só para demonstração). */
    public static final String SENHA_FORNECEDOR = "fornecedor123";

    private record EmpresaDef(
            String cnpj, String nome, String telefone, String descricao,
            String logoUrl, String capaUrl,
            String emailLogin, String nomeResponsavel,
            String logradouro, String numero, String complemento, String bairro, String cep
    ) {}

    private record ProdutoDef(
            String codigo, String cnpj, String nome, BigDecimal preco, String descricao,
            String imagemUrl, int estoqueInicial, BigDecimal precoPalete, int unidadesPorPalete
    ) {}

    private static final List<EmpresaDef> EMPRESAS = List.of(
            new EmpresaDef("30.004.004/0001-04", "Fonte Pura Distribuidora de Águas", "(81) 99900-4004",
                    "Distribuidora de águas minerais: garrafas e fardos com e sem gás para eventos e comércios do Agreste.",
                    "/uploads/empresas/fonte-pura-logo.png", "/uploads/empresas/fonte-pura-capa.png",
                    "fontepura@stock4event.com", "Fonte Pura",
                    "Av. Rui Barbosa", "1200", "Galpão 2", "Indianópolis", "55024-740"),
            new EmpresaDef("30.005.005/0001-05", "Refri Agreste Distribuidora", "(81) 99900-5005",
                    "Refrigerantes em lata, PET e fardos com entrega rápida ou retirada no galpão.",
                    "/uploads/empresas/refri-agreste-logo.png", "/uploads/empresas/refri-agreste-capa.png",
                    "refriagreste@stock4event.com", "Refri Agreste",
                    "Rodovia BR-104", "Km 62", "Galpão 5", "Distrito Industrial", "55032-000"),
            new EmpresaDef("30.006.006/0001-06", "Pomar Nordeste Sucos", "(81) 99900-6006",
                    "Sucos, néctares e polpas de frutas para festas, buffets e lanchonetes.",
                    "/uploads/empresas/pomar-nordeste-logo.png", "/uploads/empresas/pomar-nordeste-capa.png",
                    "pomarnordeste@stock4event.com", "Pomar Nordeste",
                    "Rua Vigário Freire", "455", null, "Centro", "55002-010"),
            new EmpresaDef("30.007.007/0001-07", "Adega Vale do São Francisco", "(81) 99900-7007",
                    "Vinhos tintos e brancos do Chile e de Portugal, por garrafa ou palete fechado.",
                    "/uploads/empresas/adega-sao-francisco-logo.png", "/uploads/empresas/adega-sao-francisco-capa.png",
                    "adegavsf@stock4event.com", "Adega Vale do São Francisco",
                    "Av. Agamenon Magalhães", "2890", "Loja 3", "Maurício de Nassau", "55014-000"),
            new EmpresaDef("30.008.008/0001-08", "Agreste Mix Bebidas", "(81) 99900-8008",
                    "Atacado de bebidas variadas: águas, refrigerantes, sucos e vinhos num só pedido.",
                    "/uploads/empresas/agreste-mix-logo.png", "/uploads/empresas/agreste-mix-capa.png",
                    "agrestemix@stock4event.com", "Agreste Mix",
                    "Rua José Florêncio Filho", "77", null, "Universitário", "55016-350")
    );

    /** Fotos dos produtos: Open Food Facts (openfoodfacts.org), licença CC BY-SA. */
    private static final String FOTO = "https://images.openfoodfacts.org/images/products/";

    private static final List<ProdutoDef> PRODUTOS = List.of(
            new ProdutoDef("MKT-FP-001", "30.004.004/0001-04",
                    "Água Mineral Crystal sem gás 500ml", bd("1.49"),
                    "Água mineral natural sem gás, garrafa 500 ml. Palete com 84 fardos de 12.",
                    FOTO + "789/490/053/0001/front_pt.30.400.jpg", 3000, bd("1350.00"), 1008),
            new ProdutoDef("MKT-FP-002", "30.004.004/0001-04",
                    "Água Mineral Santa Justina sem gás 500ml", bd("1.79"),
                    "Água mineral natural sem gás, garrafa 500 ml. Palete com 84 fardos de 12.",
                    FOTO + "789/896/665/5005/front_pt.4.400.jpg", 2500, bd("1600.00"), 1008),
            new ProdutoDef("MKT-FP-003", "30.004.004/0001-04",
                    "Água Mineral Minalba sem gás 510ml", bd("1.69"),
                    "Água mineral natural sem gás, garrafa 510 ml. Palete com 84 fardos de 12.",
                    FOTO + "789/606/588/0069/front_pt.3.400.jpg", 2500, bd("1500.00"), 1008),
            new ProdutoDef("MKT-FP-004", "30.004.004/0001-04",
                    "Água Mineral São Lourenço sem gás 510ml", bd("2.49"),
                    "Água mineral natural sem gás, garrafa 510 ml. Palete com 84 fardos de 12.",
                    FOTO + "789/606/280/2354/front_pt.7.400.jpg", 2000, bd("2150.00"), 1008),
            new ProdutoDef("MKT-RA-001", "30.005.005/0001-05",
                    "Refrigerante Coca-Cola Lata 350ml", bd("3.49"),
                    "Refrigerante de cola, lata 350 ml. Palete com 84 fardos de 24 latas.",
                    FOTO + "789/490/001/0015/front_pt.54.400.jpg", 5000, bd("6450.00"), 2016),
            new ProdutoDef("MKT-RA-002", "30.005.005/0001-05",
                    "Refrigerante Coca-Cola Garrafa 2L", bd("8.99"),
                    "Refrigerante de cola, garrafa PET 2 litros. Palete com 80 fardos de 6.",
                    FOTO + "789/490/002/7013/front_pt.60.400.jpg", 1800, bd("3950.00"), 480),
            new ProdutoDef("MKT-RA-003", "30.005.005/0001-05",
                    "Refrigerante Guaraná Antarctica 2L", bd("7.49"),
                    "Refrigerante de guaraná, garrafa PET 2 litros. Palete com 80 fardos de 6.",
                    FOTO + "789/199/100/1342/front_pt.11.400.jpg", 1800, bd("3250.00"), 480),
            new ProdutoDef("MKT-RA-004", "30.005.005/0001-05",
                    "Refrigerante Fanta Laranja Lata 350ml", bd("3.19"),
                    "Refrigerante sabor laranja, lata 350 ml. Palete com 84 fardos de 24 latas.",
                    FOTO + "789/490/003/0013/front_pt.30.400.jpg", 4000, bd("5900.00"), 2016),
            new ProdutoDef("MKT-PN-001", "30.006.006/0001-06",
                    "Néctar de Uva Del Valle 1L", bd("7.99"),
                    "Néctar de uva em caixa 1 litro. Palete com 60 caixas de 12.",
                    FOTO + "789/834/143/0098/front_pt.18.400.jpg", 2000, bd("5200.00"), 720),
            new ProdutoDef("MKT-PN-002", "30.006.006/0001-06",
                    "Suco Concentrado de Maracujá Maguary 500ml", bd("9.49"),
                    "Suco concentrado de maracujá, garrafa 500 ml (rende até 3 litros). Palete com 50 caixas de 12.",
                    FOTO + "789/600/055/5359/front_en.29.400.jpg", 1500, bd("5100.00"), 600),
            new ProdutoDef("MKT-PN-003", "30.006.006/0001-06",
                    "Suco de Laranja Integral Natural One 900ml", bd("14.90"),
                    "Suco de laranja integral, sem adição de açúcar, garrafa 900 ml. Palete com 80 caixas de 6.",
                    FOTO + "789/855/344/2933/front_pt.10.400.jpg", 1000, bd("6500.00"), 480),
            new ProdutoDef("MKT-PN-004", "30.006.006/0001-06",
                    "Néctar de Caju Dafruta 1L", bd("6.49"),
                    "Néctar de caju em caixa 1 litro. Palete com 60 caixas de 12.",
                    FOTO + "789/600/540/1279/front_fr.11.400.jpg", 2000, bd("4200.00"), 720),
            new ProdutoDef("MKT-VS-001", "30.007.007/0001-07",
                    "Vinho Tinto Casillero del Diablo Cabernet Sauvignon 750ml", bd("59.90"),
                    "Vinho tinto chileno Cabernet Sauvignon Reserva, garrafa 750 ml. Palete com 100 caixas de 6.",
                    FOTO + "780/432/030/3178/front_es.37.400.jpg", 600, bd("31500.00"), 600),
            new ProdutoDef("MKT-VS-002", "30.007.007/0001-07",
                    "Vinho Tinto Porta 6 750ml", bd("69.90"),
                    "Vinho tinto português da região de Lisboa, garrafa 750 ml. Palete com 100 caixas de 6.",
                    FOTO + "560/199/654/7897/front_pt.11.400.jpg", 600, bd("36000.00"), 600),
            new ProdutoDef("MKT-VS-003", "30.007.007/0001-07",
                    "Vinho Branco Casillero del Diablo Sauvignon Blanc 750ml", bd("59.90"),
                    "Vinho branco chileno Sauvignon Blanc Reserva, garrafa 750 ml. Palete com 100 caixas de 6.",
                    FOTO + "780/432/030/1174/front_en.5.400.jpg", 600, bd("31500.00"), 600),
            new ProdutoDef("MKT-VS-004", "30.007.007/0001-07",
                    "Vinho Tinto Porta da Ravessa 750ml", bd("39.90"),
                    "Vinho tinto português do Alentejo, garrafa 750 ml. Palete com 100 caixas de 6.",
                    FOTO + "560/135/601/3062/front_pt.20.400.jpg", 600, bd("21000.00"), 600),
            new ProdutoDef("MKT-AM-001", "30.008.008/0001-08",
                    "Água Mineral São Lourenço com gás 510ml", bd("3.49"),
                    "Água mineral naturalmente gasosa, garrafa 510 ml. Palete com 84 fardos de 12.",
                    FOTO + "789/606/280/1050/front_pt.7.400.jpg", 2000, bd("2800.00"), 1008),
            new ProdutoDef("MKT-AM-002", "30.008.008/0001-08",
                    "Refrigerante Pepsi Garrafa 2L", bd("6.99"),
                    "Refrigerante de cola, garrafa PET 2 litros. Palete com 80 fardos de 6.",
                    FOTO + "789/284/080/0000/front_pt.3.400.jpg", 1500, bd("3050.00"), 480),
            new ProdutoDef("MKT-AM-003", "30.008.008/0001-08",
                    "Suco de Uva Integral Salton 1,5L", bd("19.90"),
                    "Suco de uva integral, sem adição de açúcar, garrafa 1,5 litro. Palete com 80 caixas de 6.",
                    FOTO + "789/602/301/4819/front_pt.8.400.jpg", 1200, bd("7900.00"), 480),
            new ProdutoDef("MKT-AM-004", "30.008.008/0001-08",
                    "Vinho Frisante Tinto Suave Saint Germain 750ml", bd("22.90"),
                    "Vinho frisante tinto suave, garrafa 750 ml. Palete com 100 caixas de 6.",
                    FOTO + "789/114/102/6751/front_pt.11.400.jpg", 800, bd("11000.00"), 600)
    );

    /** Ilustrações 2D que o catálogo usava antes das fotos reais: são sempre trocadas. */
    private static final Set<String> ILUSTRACOES_ANTIGAS = Set.of(
            "/uploads/produtos/agua-mineral-crystal-sem-gas-500ml.png",
            "/uploads/produtos/agua-mineral-bonafont-sem-gas-500ml.png",
            "/uploads/produtos/agua-mineral-minalba-sem-gas-1-5l.png",
            "/uploads/produtos/agua-mineral-indaia-galao-20l.png",
            "/uploads/produtos/refrigerante-coca-cola-lata-350ml.png",
            "/uploads/produtos/refrigerante-coca-cola-garrafa-2l.png",
            "/uploads/produtos/refrigerante-guarana-antarctica-2l.png",
            "/uploads/produtos/refrigerante-fanta-laranja-lata-350ml.png",
            "/uploads/produtos/nectar-de-uva-del-valle-1l.png",
            "/uploads/produtos/suco-concentrado-de-maracuja-maguary-500ml.png",
            "/uploads/produtos/suco-de-laranja-integral-natural-one-900ml.png",
            "/uploads/produtos/nectar-de-caju-dafruta-1l.png",
            "/uploads/produtos/vinho-tinto-casillero-del-diablo-cabernet-sauvignon-750ml.png",
            "/uploads/produtos/vinho-tinto-miolo-selecao-cabernet-merlot-750ml.png",
            "/uploads/produtos/vinho-espumante-salton-brut-750ml.png",
            "/uploads/produtos/vinho-tinto-rio-sol-shiraz-vale-do-sao-francisco-750ml.png",
            "/uploads/produtos/agua-mineral-sao-lourenco-com-gas-300ml.png",
            "/uploads/produtos/refrigerante-pepsi-garrafa-2l.png",
            "/uploads/produtos/suco-de-laranja-tial-1l.png",
            "/uploads/produtos/vinho-tinto-suave-pergola-1l.png"
    );

    private final EmpresaRepository empresaRepository;
    private final EnderecoEntregaRepository enderecoRepository;
    private final ProdutoRepository produtoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final JdbcTemplate jdbcTemplate;

    public CatalogoBebidasSeedRunner(
            EmpresaRepository empresaRepository,
            EnderecoEntregaRepository enderecoRepository,
            ProdutoRepository produtoRepository,
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            JdbcTemplate jdbcTemplate) {
        this.empresaRepository = empresaRepository;
        this.enderecoRepository = enderecoRepository;
        this.produtoRepository = produtoRepository;
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Mantém os ids novos acima dos ids fixos do ProdutoCatalogoFixRunner (1001–1025).
        jdbcTemplate.execute("""
                SELECT setval(
                    pg_get_serial_sequence('produtos', 'id'),
                    GREATEST(COALESCE((SELECT MAX(id) FROM produtos), 1), 1025)
                )
                """);

        Perfil perfilAdmin = perfilRepository.findByNomeIgnoreCase("Admin").orElse(null);
        int empresasNovas = 0;
        for (EmpresaDef def : EMPRESAS) {
            if (!empresaRepository.existsByCnpj(def.cnpj())) {
                empresasNovas++;
            }
            Empresa empresa = sincronizarEmpresa(def);
            garantirEndereco(empresa, def);
            garantirLogin(empresa, def, perfilAdmin);
        }

        int produtosNovos = 0;
        for (ProdutoDef def : PRODUTOS) {
            Empresa empresa = empresaRepository.findByCnpj(def.cnpj()).orElse(null);
            if (empresa == null) {
                continue;
            }
            if (sincronizarProduto(empresa, def)) {
                produtosNovos++;
            }
        }
        log.info("Catálogo de bebidas: {} distribuidoras novas, {} produtos novos ({} no total).",
                empresasNovas, produtosNovos, PRODUTOS.size());
    }

    private Empresa sincronizarEmpresa(EmpresaDef def) {
        Empresa empresa = empresaRepository.findByCnpj(def.cnpj()).orElseGet(Empresa::new);
        empresa.setCnpj(def.cnpj());
        empresa.setNome(def.nome());
        empresa.setTipo("DISTRIBUIDOR");
        if (empresa.getTelefone() == null || empresa.getTelefone().isBlank()) {
            empresa.setTelefone(def.telefone());
        }
        empresa.setDescricao(def.descricao());
        empresa.setLogoUrl(def.logoUrl());
        empresa.setCapaUrl(def.capaUrl());
        return empresaRepository.save(empresa);
    }

    /** Endereço principal: usado no resumo da retirada no local. */
    private void garantirEndereco(Empresa empresa, EmpresaDef def) {
        if (enderecoRepository.countByEmpresaId(empresa.getId()) > 0) {
            return;
        }
        EnderecoEntrega endereco = new EnderecoEntrega();
        endereco.setEmpresa(empresa);
        endereco.setApelido("Galpão");
        endereco.setLogradouro(def.logradouro());
        endereco.setNumero(def.numero());
        endereco.setComplemento(def.complemento());
        endereco.setBairro(def.bairro());
        endereco.setCidade("Caruaru");
        endereco.setUf("PE");
        endereco.setCep(def.cep());
        endereco.setPrincipal(true);
        enderecoRepository.save(endereco);
    }

    /** Conta de fornecedor para entrar no app e atualizar as etapas dos pedidos. */
    private void garantirLogin(Empresa empresa, EmpresaDef def, Perfil perfilAdmin) {
        if (perfilAdmin == null || usuarioRepository.findByEmailIgnoreCase(def.emailLogin()).isPresent()) {
            return;
        }
        Usuario usuario = new Usuario();
        usuario.setNome(def.nomeResponsavel());
        usuario.setEmail(def.emailLogin());
        usuario.setSenhaHash(BCrypt.hashpw(SENHA_FORNECEDOR, BCrypt.gensalt()));
        usuario.setEmpresa(empresa);
        usuario.setPerfil(perfilAdmin);
        usuario.setPerfilUso("Fornecedor");
        usuario.setAtivo(1);
        usuarioRepository.save(usuario);
    }

    /** @return true quando o produto foi criado agora. */
    private boolean sincronizarProduto(Empresa empresa, ProdutoDef def) {
        Produto produto = produtoRepository.findByCodigo(def.codigo()).orElse(null);
        boolean novo = produto == null;
        if (novo) {
            produto = new Produto();
            produto.setCodigo(def.codigo());
            produto.setEstoque(BigDecimal.valueOf(def.estoqueInicial()));
        }
        produto.setEmpresa(empresa);
        produto.setNome(def.nome());
        produto.setDescricao(def.descricao());
        produto.setPrecoVenda(def.preco());
        // Usa a foto oficial do produto. Só mantém a imagem atual quando alguém cadastrou
        // uma foto própria pelo app (upload): ilustração antiga ou foto da Open Food Facts é trocada.
        if (deveUsarFotoDoCatalogo(produto.getImagemUrl())) {
            produto.setImagemUrl(def.imagemUrl());
        }
        produto.setUnidade("UN");
        produto.setAtivo(1);
        produto.setPrecoPalete(def.precoPalete());
        produto.setUnidadesPorPalete(def.unidadesPorPalete());
        if (produto.getEstoque() == null) {
            produto.setEstoque(BigDecimal.valueOf(def.estoqueInicial()));
        }
        produtoRepository.save(produto);
        return novo;
    }

    static boolean deveUsarFotoDoCatalogo(String imagemAtual) {
        return imagemAtual == null || imagemAtual.isBlank()
                || ILUSTRACOES_ANTIGAS.contains(imagemAtual)
                || imagemAtual.contains("openfoodfacts.org");
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
