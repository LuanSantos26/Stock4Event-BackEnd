package com.stock4event.backend.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock4event.backend.entity.Produto;
import com.stock4event.backend.repository.ProdutoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * Confere as fotos oficiais do catálogo de bebidas na Open Food Facts (base aberta de produtos;
 * fotos com licença CC BY-SA).
 *
 * O CatalogoBebidasSeedRunner já grava o link da foto de cada produto. Este runner, numa thread
 * separada depois que o servidor sobe, consulta a Open Food Facts pelo código de barras (EAN) e,
 * se a foto de frente do produto mudou de endereço, atualiza o link. A foto só é aceita quando o
 * produto encontrado é da marca certa. Foto cadastrada pelo app (upload) nunca é trocada.
 * Sem internet nada muda. Para desligar: stock4event.fotos-reais.ativo=false no application.properties.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class FotoProdutoRealRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FotoProdutoRealRunner.class);
    private static final String API = "https://world.openfoodfacts.org";
    /** Fotos dos produtos: Open Food Facts, licença CC BY-SA (citar a fonte ao divulgar). */
    private static final String USER_AGENT = "Stock4event/1.0 (projeto academico UNINASSAU)";

    /**
     * ean: código de barras do produto na Open Food Facts.
     * marca: precisa aparecer no nome ou na marca do resultado, para não pegar foto de outro produto.
     */
    private record FotoDef(String codigo, String ean, String marca) {}

    private static final List<FotoDef> FOTOS = List.of(
            new FotoDef("MKT-FP-001", "7894900530001", "crystal"),
            new FotoDef("MKT-FP-002", "7898966655005", "justina"),
            new FotoDef("MKT-FP-003", "7896065880069", "minalba"),
            new FotoDef("MKT-FP-004", "7896062802354", "lourenco"),
            new FotoDef("MKT-RA-001", "7894900010015", "coca"),
            new FotoDef("MKT-RA-002", "7894900027013", "coca"),
            new FotoDef("MKT-RA-003", "7891991001342", "guarana"),
            new FotoDef("MKT-RA-004", "7894900030013", "fanta"),
            new FotoDef("MKT-PN-001", "7898341430098", "del valle"),
            new FotoDef("MKT-PN-002", "7896000555359", "maguary"),
            new FotoDef("MKT-PN-003", "7898553442933", "natural one"),
            new FotoDef("MKT-PN-004", "7896005401279", "dafruta"),
            new FotoDef("MKT-VS-001", "7804320303178", "casillero"),
            new FotoDef("MKT-VS-002", "5601996547897", "porta 6"),
            new FotoDef("MKT-VS-003", "7804320301174", "casillero"),
            new FotoDef("MKT-VS-004", "5601356013062", "ravessa"),
            new FotoDef("MKT-AM-001", "7896062801050", "lourenco"),
            new FotoDef("MKT-AM-002", "7892840800000", "pepsi"),
            new FotoDef("MKT-AM-003", "7896023014819", "salton"),
            new FotoDef("MKT-AM-004", "7891141026751", "saint germain")
    );

    private final ProdutoRepository produtoRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Value("${stock4event.fotos-reais.ativo:true}")
    private boolean ativo;

    public FotoProdutoRealRunner(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!ativo) return;
        Thread thread = new Thread(this::atualizarFotos, "fotos-reais-produtos");
        thread.setDaemon(true);
        thread.start();
    }

    private void atualizarFotos() {
        int atualizadas = 0;
        for (FotoDef def : FOTOS) {
            Produto produto = produtoRepository.findByCodigo(def.codigo()).orElse(null);
            if (produto == null || !CatalogoBebidasSeedRunner.deveUsarFotoDoCatalogo(produto.getImagemUrl())) continue;
            try {
                String url = fotoPorEan(def.ean(), def.marca());
                if (url == null || url.equals(produto.getImagemUrl())) continue;
                produto.setImagemUrl(url);
                produtoRepository.save(produto);
                atualizadas++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                // Sem internet ou API fora do ar: mantém os links gravados e tenta no próximo reinício.
                log.info("Fotos reais: não foi possível acessar a Open Food Facts ({}). Mantidos os links atuais.",
                        e.getClass().getSimpleName());
                return;
            }
        }
        if (atualizadas > 0) {
            log.info("Fotos reais: {} links de foto atualizados pela Open Food Facts.", atualizadas);
        }
    }

    private String fotoPorEan(String ean, String marca) throws Exception {
        JsonNode raiz = get(API + "/api/v2/product/" + ean + ".json?fields=product_name,brands,image_front_url");
        if (raiz == null || raiz.path("status").asInt(0) != 1) return null;
        return fotoSeForDaMarca(raiz.path("product"), marca);
    }

    /** A foto só vale se o produto encontrado for da marca certa e tiver imagem de frente. */
    static String fotoSeForDaMarca(JsonNode produto, String marca) {
        String url = produto.path("image_front_url").asText("");
        if (!url.startsWith("https://")) return null;
        String texto = normalizar(produto.path("product_name").asText("") + " " + produto.path("brands").asText(""));
        return texto.contains(normalizar(marca)) ? url : null;
    }

    static String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('-', ' ')
                .toLowerCase(Locale.ROOT);
    }

    private JsonNode get(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) return null;
        return objectMapper.readTree(resp.body());
    }
}
