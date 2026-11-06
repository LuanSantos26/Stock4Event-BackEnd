package com.stock4event.backend.service;

import com.stock4event.backend.entity.Produto;
import com.stock4event.backend.exception.CadastroException;
import com.stock4event.backend.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Regras do código de barras dos produtos (lido no PDV).
 *
 * Aceita EAN-8, UPC-A (12), EAN-13 e GTIN-14, só dígitos e com dígito verificador correto.
 * Quando o produto não tem código, gera um EAN-13 de uso interno (prefixo 2, faixa reservada
 * para uso da própria loja), a partir da empresa e do id do produto.
 */
@Service
public class CodigoBarrasService {

    @Autowired private ProdutoRepository produtoRepository;

    /** Só dígitos; vazio vira null. */
    public static String normalizar(String codigo) {
        if (codigo == null) return null;
        String digitos = codigo.replaceAll("\\D", "");
        return digitos.isEmpty() ? null : digitos;
    }

    /** Dígito verificador GS1 (EAN/UPC/GTIN) para os dígitos sem o verificador. */
    public static int digitoVerificador(String semDigito) {
        int soma = 0;
        for (int i = 0; i < semDigito.length(); i++) {
            int d = semDigito.charAt(semDigito.length() - 1 - i) - '0';
            soma += (i % 2 == 0) ? d * 3 : d;
        }
        return (10 - (soma % 10)) % 10;
    }

    public static boolean valido(String codigo) {
        if (codigo == null || !codigo.matches("\\d+")) return false;
        int n = codigo.length();
        if (n != 8 && n != 12 && n != 13 && n != 14) return false;
        int esperado = digitoVerificador(codigo.substring(0, n - 1));
        return esperado == codigo.charAt(n - 1) - '0';
    }

    /** EAN-13 interno: 2 + empresa (4 dígitos) + produto (7 dígitos) + verificador. */
    public static String gerarInterno(Long empresaId, Long produtoId) {
        String base = "2"
                + String.format("%04d", empresaId % 10_000)
                + String.format("%07d", produtoId % 10_000_000);
        return base + digitoVerificador(base);
    }

    /** Valida o código informado no cadastro e garante que não se repete na mesma empresa. */
    public String validarParaEmpresa(String informado, Long empresaId, Long produtoId) {
        String codigo = normalizar(informado);
        if (codigo == null) return null;
        if (!valido(codigo)) {
            throw new CadastroException(HttpStatus.BAD_REQUEST,
                    "Código de barras inválido. Use EAN-8, EAN-13, UPC ou GTIN-14 (confira o último dígito).");
        }
        produtoRepository.findFirstByEmpresaIdAndCodigoBarras(empresaId, codigo)
                .filter(outro -> produtoId == null || !outro.getId().equals(produtoId))
                .ifPresent(outro -> {
                    throw new CadastroException(HttpStatus.BAD_REQUEST,
                            "Código de barras já usado em \"" + outro.getNome() + "\".");
                });
        return codigo;
    }

    /** Se o produto (já salvo) ainda não tem código, gera o interno e salva. */
    public Produto garantirCodigo(Produto produto) {
        if (produto.getCodigoBarras() == null || produto.getCodigoBarras().isBlank()) {
            produto.setCodigoBarras(gerarInterno(produto.getEmpresa().getId(), produto.getId()));
            return produtoRepository.save(produto);
        }
        return produto;
    }
}
