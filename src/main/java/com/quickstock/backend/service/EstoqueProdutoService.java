package com.quickstock.backend.service;

import com.quickstock.backend.entity.Empresa;
import com.quickstock.backend.entity.ItemSolicitacaoCompra;
import com.quickstock.backend.entity.Produto;
import com.quickstock.backend.entity.SolicitacaoCompra;
import com.quickstock.backend.exception.CadastroException;
import com.quickstock.backend.repository.ItemSolicitacaoCompraRepository;
import com.quickstock.backend.repository.ProdutoRepository;
import com.quickstock.backend.repository.SolicitacaoCompraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class EstoqueProdutoService {

    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private ItemSolicitacaoCompraRepository itemSolicitacaoRepository;
    @Autowired private SolicitacaoCompraRepository solicitacaoRepository;

    public String gerarCodigoCatalogo(Long empresaId) {
        long seq = produtoRepository.countByEmpresaId(empresaId) + 1;
        String codigo;
        do {
            codigo = String.format("EMP-%d-%04d", empresaId, seq);
            seq++;
        } while (produtoRepository.findByCodigo(codigo).isPresent());
        return codigo;
    }

    @Transactional
    public void debitarEstoque(Produto produto, BigDecimal quantidade) {
        BigDecimal estoqueAtual = produto.getEstoque() != null ? produto.getEstoque() : BigDecimal.ZERO;
        if (estoqueAtual.compareTo(quantidade) < 0) {
            throw new CadastroException(
                    HttpStatus.BAD_REQUEST,
                    "Estoque insuficiente para \"" + produto.getNome() + "\". Disponível: "
                            + estoqueAtual.stripTrailingZeros().toPlainString()
            );
        }
        produto.setEstoque(estoqueAtual.subtract(quantidade));
        produtoRepository.save(produto);
    }

    /** Devolve ao fornecedor o estoque de uma solicitação cancelada. */
    @Transactional
    public void devolverEstoqueFornecedor(SolicitacaoCompra solicitacao) {
        for (ItemSolicitacaoCompra item : itemSolicitacaoRepository.findBySolicitacaoId(solicitacao.getId())) {
            Produto produto = item.getProduto();
            BigDecimal estoqueAtual = produto.getEstoque() != null ? produto.getEstoque() : BigDecimal.ZERO;
            produto.setEstoque(estoqueAtual.add(item.getQuantidade()));
            produtoRepository.save(produto);
        }
    }

    @Transactional
    public void creditarCompradorSeNecessario(SolicitacaoCompra solicitacao) {
        if (Boolean.TRUE.equals(solicitacao.getEstoqueCompradorCreditado())) {
            return;
        }

        List<ItemSolicitacaoCompra> itens = itemSolicitacaoRepository.findBySolicitacaoId(solicitacao.getId());
        Empresa compradora = solicitacao.getEmpresaCompradora();

        for (ItemSolicitacaoCompra item : itens) {
            Produto produtoFornecedor = item.getProduto();
            BigDecimal quantidade = item.getQuantidade();

            Produto produtoComprador = produtoRepository
                    .findByEmpresaIdAndCodigoOrigem(compradora.getId(), produtoFornecedor.getCodigo())
                    .orElseGet(() -> criarProdutoRecebido(compradora, produtoFornecedor));

            BigDecimal estoqueAtual = produtoComprador.getEstoque() != null
                    ? produtoComprador.getEstoque()
                    : BigDecimal.ZERO;
            produtoComprador.setEstoque(estoqueAtual.add(quantidade));
            produtoRepository.save(produtoComprador);
        }

        solicitacao.setEstoqueCompradorCreditado(true);
        solicitacaoRepository.save(solicitacao);
    }

    private Produto criarProdutoRecebido(Empresa compradora, Produto produtoFornecedor) {
        Produto produto = new Produto();
        produto.setEmpresa(compradora);
        produto.setNome(produtoFornecedor.getNome());
        produto.setPrecoVenda(produtoFornecedor.getPrecoVenda());
        produto.setUnidade(produtoFornecedor.getUnidade());
        produto.setDescricao(produtoFornecedor.getDescricao());
        produto.setImagemUrl(produtoFornecedor.getImagemUrl());
        produto.setCodigo(gerarCodigoCatalogo(compradora.getId()));
        produto.setCodigoOrigem(produtoFornecedor.getCodigo());
        produto.setEstoque(BigDecimal.ZERO);
        produto.setAtivo(1);
        return produtoRepository.save(produto);
    }
}
