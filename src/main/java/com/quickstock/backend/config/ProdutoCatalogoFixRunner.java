package com.quickstock.backend.config;

import com.quickstock.backend.entity.Empresa;
import com.quickstock.backend.entity.Produto;
import com.quickstock.backend.repository.EmpresaRepository;
import com.quickstock.backend.repository.ProdutoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class ProdutoCatalogoFixRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProdutoCatalogoFixRunner.class);

    private record CatalogoDef(
            long idFixo,
            String codigo,
            String cnpj,
            String nome,
            BigDecimal preco,
            String descricao,
            String imagemUrl,
            int estoqueInicial
    ) {}

    private static final List<CatalogoDef> CATALOGO = List.of(
            new CatalogoDef(1001, "MKT-CDV-001", "30.001.001/0001-01",
                    "Vinho Mazzei Zisola Sicilia Noto Rosso 750ml", bd("156.00"),
                    "Vinho tinto siciliano Noto Rosso, garrafa 750 ml.",
                    "/uploads/produtos/vinho-mazzei-zisola.png", 150),
            new CatalogoDef(1002, "MKT-CDV-002", "30.001.001/0001-01",
                    "Vinho Louis Latour Bourgogne Chardonnay 750 ml", bd("200.00"),
                    "Vinho branco Bourgogne Chardonnay, garrafa 750 ml.",
                    "/uploads/produtos/vinho-louis-latour.png", 100),
            new CatalogoDef(1003, "MKT-CDV-003", "30.001.001/0001-01",
                    "Vinho Naturelle Tinto Reserva 750 ML", bd("60.00"),
                    "Vinho tinto reserva Naturelle, garrafa 750 ml.",
                    "/uploads/produtos/vinho-naturelle-tinto-reserva.png", 300),
            new CatalogoDef(1004, "MKT-CDV-004", "30.001.001/0001-01",
                    "Vinho Carmin De Peumo Carmenere 750 ml", bd("666.00"),
                    "Vinho tinto Carmenere Carmin De Peumo, garrafa 750 ml.",
                    "/uploads/produtos/vinho-carmin-de-peumo.png", 80),
            new CatalogoDef(1005, "MKT-CDV-005", "30.001.001/0001-01",
                    "Vinho Carolina Reserva Sauvignon Blanc 750 ml", bd("60.00"),
                    "Vinho branco Sauvignon Blanc reserva Carolina, garrafa 750 ml.",
                    "/uploads/produtos/vinho-carolina-sauvignon-blanc.png", 200),

            new CatalogoDef(1011, "MKT-CC-001", "30.002.002/0001-02",
                    "Cerveja Heineken Long Neck 330ml", bd("5.99"),
                    "Cerveja Heineken long neck 330 ml.",
                    "/uploads/produtos/cerveja-heineken-long-neck.png", 200),
            new CatalogoDef(1012, "MKT-CC-002", "30.002.002/0001-02",
                    "Cerveja Corona Long Neck", bd("6.99"),
                    "Cerveja Corona Extra long neck 330 ml.",
                    "/uploads/produtos/cerveja-corona-long-neck.png", 150),
            new CatalogoDef(1013, "MKT-CC-003", "30.002.002/0001-02",
                    "Cerveja Amstel Lata 269ml", bd("3.19"),
                    "Cerveja Amstel lata 269 ml.",
                    "/uploads/produtos/cerveja-amstel-lata.png", 300),
            new CatalogoDef(1014, "MKT-CC-004", "30.002.002/0001-02",
                    "Cerveja Cerpa Export Long Neck 350ml", bd("8.39"),
                    "Cerveja Cerpa Export long neck 350 ml.",
                    "/uploads/produtos/cerveja-cerpa-export-long-neck.png", 100),
            new CatalogoDef(1015, "MKT-CC-005", "30.002.002/0001-02",
                    "Cerveja Paulaner Munchen Weissbier 500ml", bd("11.19"),
                    "Cerveja Paulaner Munchen Weissbier garrafa 500 ml.",
                    "/uploads/produtos/cerveja-paulaner-munchen-weissbier.png", 80),

            new CatalogoDef(1021, "MKT-WL-001", "30.003.003/0001-03",
                    "Whisky Escocês Royal Salute 21 Anos The Signature Blend 700ml", bd("989.00"),
                    "Whisky escocês Royal Salute 21 anos, garrafa 700 ml.",
                    "/uploads/produtos/whisky-royal-salute-21.png", 150),
            new CatalogoDef(1022, "MKT-WL-002", "30.003.003/0001-03",
                    "Whisky The Balvenie PortWood 21 Anos 700ml Single Malt Escocês", bd("2795.30"),
                    "Whisky single malt The Balvenie PortWood 21 anos, garrafa 700 ml.",
                    "/uploads/produtos/whisky-balvenie-portwood-21.png", 100),
            new CatalogoDef(1023, "MKT-WL-003", "30.003.003/0001-03",
                    "Whisky Glenfiddich Single Malt 12 Anos 750ml Escocês", bd("415.75"),
                    "Whisky single malt Glenfiddich 12 anos, garrafa 750 ml.",
                    "/uploads/produtos/whisky-glenfiddich-12.png", 300),
            new CatalogoDef(1024, "MKT-WL-004", "30.003.003/0001-03",
                    "Conhaque Martell L'Or de Jean Martell 700ml", bd("18711.00"),
                    "Conhaque Martell L'Or de Jean Martell, garrafa 700 ml.",
                    "/uploads/produtos/conhaque-martell-lor.png", 80),
            new CatalogoDef(1025, "MKT-WL-005", "30.003.003/0001-03",
                    "Whisky Suntory Hibiki Japanese Harmony 700ml | Blended Premium Japonês", bd("729.90"),
                    "Whisky japonês Suntory Hibiki Japanese Harmony, garrafa 700 ml.",
                    "/uploads/produtos/whisky-hibiki-japanese-harmony.png", 200)
    );

    private final EmpresaRepository empresaRepository;
    private final ProdutoRepository produtoRepository;
    private final JdbcTemplate jdbcTemplate;

    public ProdutoCatalogoFixRunner(
            EmpresaRepository empresaRepository,
            ProdutoRepository produtoRepository,
            JdbcTemplate jdbcTemplate) {
        this.empresaRepository = empresaRepository;
        this.produtoRepository = produtoRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        garantirCodigosLegados();
        int sincronizados = 0;

        for (CatalogoDef def : CATALOGO) {
            Optional<Empresa> empresaOpt = empresaRepository.findByCnpj(def.cnpj());
            if (empresaOpt.isEmpty()) {
                continue;
            }
            sincronizarProduto(empresaOpt.get(), def);
            sincronizados++;
        }

        ajustarSequenciaIds();
        log.info("Catálogo fixo: {} produtos de parceiros sincronizados.", sincronizados);
    }

    private void garantirCodigosLegados() {
        produtoRepository.findAll().stream()
                .filter(p -> p.getCodigo() == null || p.getCodigo().isBlank())
                .forEach(p -> {
                    p.setCodigo("LEG-" + p.getId());
                    produtoRepository.save(p);
                });
    }

    private void sincronizarProduto(Empresa empresa, CatalogoDef def) {
        Optional<Produto> existente = produtoRepository.findByCodigo(def.codigo());
        if (existente.isEmpty()) {
            existente = produtoRepository.findById(def.idFixo());
        }
        if (existente.isEmpty()) {
            existente = produtoRepository.findByEmpresaIdAndAtivo(empresa.getId(), 1).stream()
                    .filter(p -> def.nome().equals(p.getNome()))
                    .findFirst();
        }

        if (existente.isPresent()) {
            Produto produto = existente.get();
            produto.setCodigo(def.codigo());
            produto.setNome(def.nome());
            produto.setDescricao(def.descricao());
            produto.setPrecoVenda(def.preco());
            produto.setImagemUrl(def.imagemUrl());
            produto.setUnidade("UN");
            produto.setAtivo(1);
            if (produto.getEstoque() == null) {
                produto.setEstoque(BigDecimal.valueOf(def.estoqueInicial()));
            }
            produtoRepository.save(produto);
            return;
        }

        jdbcTemplate.update("""
                INSERT INTO produtos (id, codigo, empresa_id, nome, preco_venda, unidade, descricao, imagem_url, ativo, estoque)
                VALUES (?, ?, ?, ?, ?, 'UN', ?, ?, 1, ?)
                ON CONFLICT (id) DO NOTHING
                """,
                def.idFixo(),
                def.codigo(),
                empresa.getId(),
                def.nome(),
                def.preco(),
                def.descricao(),
                def.imagemUrl(),
                BigDecimal.valueOf(def.estoqueInicial()));
    }

    private void ajustarSequenciaIds() {
        jdbcTemplate.execute("""
                SELECT setval(
                    pg_get_serial_sequence('produtos', 'id'),
                    GREATEST(COALESCE((SELECT MAX(id) FROM produtos), 1), 1025)
                )
                """);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
