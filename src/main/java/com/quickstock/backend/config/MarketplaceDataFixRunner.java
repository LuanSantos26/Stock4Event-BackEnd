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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class MarketplaceDataFixRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceDataFixRunner.class);

    private final EmpresaRepository empresaRepository;
    private final ProdutoRepository produtoRepository;

    public MarketplaceDataFixRunner(
            EmpresaRepository empresaRepository,
            ProdutoRepository produtoRepository) {
        this.empresaRepository = empresaRepository;
        this.produtoRepository = produtoRepository;
    }

    private record EmpresaFix(String nome, String descricao, String logoUrl, String capaUrl) {}

    private record ProdutoFix(String codigo, String nome, BigDecimal preco, String descricao, String imagemUrl, int estoqueInicial) {}

    private static String logo(String initials, String bg) {
        return "https://ui-avatars.com/api/?name=" + initials
                + "&background=" + bg + "&color=fff&bold=true&size=200&format=png";
    }

    private static String capa(String seed) {
        return "https://picsum.photos/seed/" + seed + "/800/400";
    }

    private static String img(String seed) {
        return "https://picsum.photos/seed/" + seed + "/400/400";
    }

    private static final String LOGO_CASA_DOS_VINHOS = "/uploads/empresas/casa-dos-vinhos-logo.png";
    private static final String CAPA_CASA_DOS_VINHOS = "/uploads/empresas/casa-dos-vinhos-capa.png";
    private static final String LOGO_WHISKY_LABS = "/uploads/empresas/whisky-labs-logo.png";
    private static final String CAPA_WHISKY_LABS = "/uploads/empresas/whisky-labs-capa.png";
    private static final String LOGO_CERVEJARIA_CARUARU = "/uploads/empresas/cervejaria-caruaru-logo.png";
    private static final String CAPA_CERVEJARIA_CARUARU = "/uploads/empresas/cervejaria-caruaru-capa.png";
    private static final String IMG_CERVEJA_HEINEKEN = "/uploads/produtos/cerveja-heineken-long-neck.png";
    private static final String IMG_CERVEJA_CORONA = "/uploads/produtos/cerveja-corona-long-neck.png";
    private static final String IMG_CERVEJA_AMSTEL = "/uploads/produtos/cerveja-amstel-lata.png";
    private static final String IMG_CERVEJA_CERPA = "/uploads/produtos/cerveja-cerpa-export-long-neck.png";
    private static final String IMG_CERVEJA_PAULANER = "/uploads/produtos/cerveja-paulaner-munchen-weissbier.png";
    private static final String IMG_VINHO_MAZZEI = "/uploads/produtos/vinho-mazzei-zisola.png";
    private static final String IMG_VINHO_LOUIS_LATOUR = "/uploads/produtos/vinho-louis-latour.png";
    private static final String IMG_VINHO_NATURELLE = "/uploads/produtos/vinho-naturelle-tinto-reserva.png";
    private static final String IMG_VINHO_CARMIN = "/uploads/produtos/vinho-carmin-de-peumo.png";
    private static final String IMG_VINHO_CAROLINA = "/uploads/produtos/vinho-carolina-sauvignon-blanc.png";
    private static final String IMG_CONHAQUE_MARTELL = "/uploads/produtos/conhaque-martell-lor.png";
    private static final String IMG_WHISKY_ROYAL_SALUTE = "/uploads/produtos/whisky-royal-salute-21.png";
    private static final String IMG_WHISKY_BALVENIE = "/uploads/produtos/whisky-balvenie-portwood-21.png";
    private static final String IMG_WHISKY_GLENFIDDICH = "/uploads/produtos/whisky-glenfiddich-12.png";
    private static final String IMG_WHISKY_HIBIKI = "/uploads/produtos/whisky-hibiki-japanese-harmony.png";

    private static final Map<String, EmpresaFix> EMPRESAS = Map.of(
            "30.001.001/0001-01", new EmpresaFix(
                    "Casa dos Vinhos",
                    "Vinhos nacionais e importados selecionados para bares, restaurantes e revenda.",
                    LOGO_CASA_DOS_VINHOS,
                    CAPA_CASA_DOS_VINHOS),
            "30.002.002/0001-02", new EmpresaFix(
                    "Cervejaria Caruaru",
                    "Cervejas geladas e importadas para revenda em Caruaru e região.",
                    LOGO_CERVEJARIA_CARUARU,
                    CAPA_CERVEJARIA_CARUARU),
            "30.003.003/0001-03", new EmpresaFix(
                    "Whisky Labs",
                    "Especialistas na distribuição de whiskies, conhaques e destilados premium para colecionadores e revendedores.",
                    LOGO_WHISKY_LABS,
                    CAPA_WHISKY_LABS)
    );

    private static final Map<String, List<ProdutoFix>> PRODUTOS_POR_CNPJ = Map.of(
            "30.001.001/0001-01", List.of(
                    new ProdutoFix("MKT-CDV-001", "Vinho Mazzei Zisola Sicilia Noto Rosso 750ml", bd("156.00"),
                            "Vinho tinto siciliano Noto Rosso, garrafa 750 ml.", IMG_VINHO_MAZZEI, 150),
                    new ProdutoFix("MKT-CDV-002", "Vinho Louis Latour Bourgogne Chardonnay 750 ml", bd("200.00"),
                            "Vinho branco Bourgogne Chardonnay, garrafa 750 ml.", IMG_VINHO_LOUIS_LATOUR, 100),
                    new ProdutoFix("MKT-CDV-003", "Vinho Naturelle Tinto Reserva 750 ML", bd("60.00"),
                            "Vinho tinto reserva Naturelle, garrafa 750 ml.", IMG_VINHO_NATURELLE, 300),
                    new ProdutoFix("MKT-CDV-004", "Vinho Carmin De Peumo Carmenere 750 ml", bd("666.00"),
                            "Vinho tinto Carmenere Carmin De Peumo, garrafa 750 ml.", IMG_VINHO_CARMIN, 80),
                    new ProdutoFix("MKT-CDV-005", "Vinho Carolina Reserva Sauvignon Blanc 750 ml", bd("60.00"),
                            "Vinho branco Sauvignon Blanc reserva Carolina, garrafa 750 ml.", IMG_VINHO_CAROLINA, 200)
            ),
            "30.002.002/0001-02", List.of(
                    new ProdutoFix("MKT-CC-001", "Cerveja Heineken Long Neck 330ml", bd("5.99"),
                            "Cerveja Heineken long neck 330 ml.", IMG_CERVEJA_HEINEKEN, 200),
                    new ProdutoFix("MKT-CC-002", "Cerveja Corona Long Neck", bd("6.99"),
                            "Cerveja Corona Extra long neck 330 ml.", IMG_CERVEJA_CORONA, 150),
                    new ProdutoFix("MKT-CC-003", "Cerveja Amstel Lata 269ml", bd("3.19"),
                            "Cerveja Amstel lata 269 ml.", IMG_CERVEJA_AMSTEL, 300),
                    new ProdutoFix("MKT-CC-004", "Cerveja Cerpa Export Long Neck 350ml", bd("8.39"),
                            "Cerveja Cerpa Export long neck 350 ml.", IMG_CERVEJA_CERPA, 100),
                    new ProdutoFix("MKT-CC-005", "Cerveja Paulaner Munchen Weissbier 500ml", bd("11.19"),
                            "Cerveja Paulaner Munchen Weissbier garrafa 500 ml.", IMG_CERVEJA_PAULANER, 80)
            ),
            "30.003.003/0001-03", List.of(
                    new ProdutoFix("MKT-WL-001", "Whisky Escocês Royal Salute 21 Anos The Signature Blend 700ml", bd("989.00"),
                            "Whisky escocês Royal Salute 21 anos, garrafa 700 ml.", IMG_WHISKY_ROYAL_SALUTE, 150),
                    new ProdutoFix("MKT-WL-002", "Whisky The Balvenie PortWood 21 Anos 700ml Single Malt Escocês", bd("2795.30"),
                            "Whisky single malt The Balvenie PortWood 21 anos, garrafa 700 ml.", IMG_WHISKY_BALVENIE, 100),
                    new ProdutoFix("MKT-WL-003", "Whisky Glenfiddich Single Malt 12 Anos 750ml Escocês", bd("415.75"),
                            "Whisky single malt Glenfiddich 12 anos, garrafa 750 ml.", IMG_WHISKY_GLENFIDDICH, 300),
                    new ProdutoFix("MKT-WL-004", "Conhaque Martell L'Or de Jean Martell 700ml", bd("18711.00"),
                            "Conhaque Martell L'Or de Jean Martell, garrafa 700 ml.", IMG_CONHAQUE_MARTELL, 80),
                    new ProdutoFix("MKT-WL-005", "Whisky Suntory Hibiki Japanese Harmony 700ml | Blended Premium Japonês", bd("729.90"),
                            "Whisky japonês Suntory Hibiki Japanese Harmony, garrafa 700 ml.", IMG_WHISKY_HIBIKI, 200)
            )
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int empresasAtualizadas = 0;
        int produtosAtualizados = 0;

        for (Map.Entry<String, EmpresaFix> entry : EMPRESAS.entrySet()) {
            Optional<Empresa> empresaOpt = empresaRepository.findByCnpj(entry.getKey());
            if (empresaOpt.isEmpty()) {
                log.warn("Marketplace seed: CNPJ {} não encontrado no banco.", entry.getKey());
                continue;
            }

            Empresa empresa = empresaOpt.get();
            aplicarFixEmpresa(empresa, entry.getValue());
            empresa.setTipo("DISTRIBUIDOR");
            empresaRepository.save(empresa);
            empresasAtualizadas++;

            List<ProdutoFix> produtos = PRODUTOS_POR_CNPJ.getOrDefault(entry.getKey(), List.of());
            for (ProdutoFix fix : produtos) {
                produtosAtualizados += sincronizarProduto(empresa, fix);
            }
            desativarProdutosForaDoCatalogo(empresa, produtos);
        }

        log.info("Marketplace seed: {} empresas e {} produtos sincronizados.", empresasAtualizadas, produtosAtualizados);
    }

    private int sincronizarProduto(Empresa empresa, ProdutoFix fix) {
        Optional<Produto> existente = produtoRepository.findByCodigo(fix.codigo());

        if (existente.isEmpty()) {
            existente = produtoRepository.findByEmpresaIdAndAtivo(empresa.getId(), 1).stream()
                    .filter(p -> fix.nome().equals(p.getNome()))
                    .findFirst();
        }

        if (existente.isEmpty()) {
            existente = produtoRepository.findAll().stream()
                    .filter(p -> empresa.getId().equals(p.getEmpresa().getId()))
                    .filter(p -> fix.nome().equals(p.getNome()))
                    .findFirst();
        }

        Produto produto = existente.orElseGet(() -> {
            Produto novo = new Produto();
            novo.setEmpresa(empresa);
            novo.setNome(fix.nome());
            novo.setUnidade("UN");
            novo.setAtivo(1);
            novo.setCodigo(fix.codigo());
            novo.setEstoque(BigDecimal.valueOf(fix.estoqueInicial()));
            return novo;
        });

        produto.setCodigo(fix.codigo());
        produto.setNome(fix.nome());
        produto.setDescricao(fix.descricao());
        produto.setPrecoVenda(fix.preco());
        produto.setImagemUrl(fix.imagemUrl());
        produto.setAtivo(1);
        if (produto.getEstoque() == null) {
            produto.setEstoque(BigDecimal.valueOf(fix.estoqueInicial()));
        }
        produtoRepository.save(produto);
        return 1;
    }

    private void desativarProdutosForaDoCatalogo(Empresa empresa, List<ProdutoFix> catalogo) {
        Set<String> nomesOficiais = catalogo.stream()
                .map(ProdutoFix::nome)
                .collect(Collectors.toCollection(HashSet::new));

        produtoRepository.findByEmpresaIdAndAtivo(empresa.getId(), 1).stream()
                .filter(p -> !nomesOficiais.contains(p.getNome()))
                .forEach(p -> {
                    p.setAtivo(0);
                    produtoRepository.save(p);
                    log.info("Marketplace seed: produto extra desativado — {} (empresa {}).", p.getNome(), empresa.getNome());
                });
    }

    private void aplicarFixEmpresa(Empresa empresa, EmpresaFix fix) {
        empresa.setNome(fix.nome());
        empresa.setDescricao(fix.descricao());
        empresa.setLogoUrl(fix.logoUrl());
        empresa.setCapaUrl(fix.capaUrl());
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
