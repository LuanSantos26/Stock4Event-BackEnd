package com.quickstock.backend.repository;

import com.quickstock.backend.entity.Produto;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    List<Produto> findByEmpresaIdAndAtivo(Long empresaId, Integer ativo);

    /** Produtos ativos de empresas fornecedoras, dos mais novos (maior id) para os mais antigos. */
    List<Produto> findByEmpresa_TipoInAndAtivoOrderByIdDesc(List<String> tipos, Integer ativo, Pageable pageable);

    Optional<Produto> findByCodigo(String codigo);

    Optional<Produto> findByEmpresaIdAndCodigoOrigem(Long empresaId, String codigoOrigem);

    long countByEmpresaId(Long empresaId);

    /** Trava o produto durante a venda do PDV, para o estoque total não ficar negativo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Produto p WHERE p.id = :id")
    Optional<Produto> findParaVenda(@Param("id") Long id);
}
