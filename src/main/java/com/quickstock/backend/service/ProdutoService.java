package com.quickstock.backend.service;

import com.quickstock.backend.entity.Produto;
import com.quickstock.backend.exception.CadastroException;
import com.quickstock.backend.repository.EstoqueBarracaRepository;
import com.quickstock.backend.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProdutoService {

    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private EstoqueBarracaRepository estoqueBarracaRepository;

    @Transactional
    public void desativar(Long id, Long empresaId) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new CadastroException(HttpStatus.NOT_FOUND, "Produto não encontrado."));

        if (empresaId != null && !produto.getEmpresa().getId().equals(empresaId)) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Produto não pertence à empresa informada.");
        }

        if (produto.getAtivo() != null && produto.getAtivo() == 0) {
            return;
        }

        produto.setAtivo(0);
        produtoRepository.save(produto);
        estoqueBarracaRepository.deleteByProdutoId(id);
    }
}
