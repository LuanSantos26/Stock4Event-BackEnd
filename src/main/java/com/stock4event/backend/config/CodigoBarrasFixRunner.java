package com.stock4event.backend.config;

import com.stock4event.backend.entity.Produto;
import com.stock4event.backend.repository.ProdutoRepository;
import com.stock4event.backend.service.CodigoBarrasService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Dá código de barras aos produtos que ainda não têm (os de exemplo e os cadastrados antes do campo existir).
 * Produto recebido de um fornecedor herda o código do original; os demais ganham um código interno.
 * Roda depois dos outros ajustes de catálogo.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class CodigoBarrasFixRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CodigoBarrasFixRunner.class);

    private final ProdutoRepository produtoRepository;

    public CodigoBarrasFixRunner(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Produto> semCodigo = new ArrayList<>(produtoRepository.findByCodigoBarrasIsNull());
        if (semCodigo.isEmpty()) return;

        // Primeiro os produtos originais, para os recebidos copiarem o código deles.
        semCodigo.sort((a, b) -> Boolean.compare(a.getCodigoOrigem() != null, b.getCodigoOrigem() != null));
        for (Produto produto : semCodigo) {
            String codigo = null;
            if (produto.getCodigoOrigem() != null) {
                codigo = produtoRepository.findByCodigo(produto.getCodigoOrigem())
                        .map(Produto::getCodigoBarras)
                        .orElse(null);
            }
            if (codigo == null) {
                codigo = CodigoBarrasService.gerarInterno(produto.getEmpresa().getId(), produto.getId());
            }
            produto.setCodigoBarras(codigo);
            produtoRepository.save(produto);
        }
        log.info("Código de barras definido para {} produto(s).", semCodigo.size());
    }
}
