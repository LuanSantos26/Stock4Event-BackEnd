package com.stock4event.backend.repository;

import com.stock4event.backend.entity.EstoqueBarraca;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstoqueBarracaRepository extends JpaRepository<EstoqueBarraca, Long> {
    List<EstoqueBarraca> findByBarracaId(Long barracaId);
    Optional<EstoqueBarraca> findByBarracaIdAndProdutoId(Long barracaId, Long produtoId);

    @Modifying
    @Query("DELETE FROM EstoqueBarraca e WHERE e.produto.id = :produtoId")
    void deleteByProdutoId(@Param("produtoId") Long produtoId);

    /** Trava a linha do estoque durante a venda do PDV, para duas vendas não usarem o mesmo saldo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EstoqueBarraca e WHERE e.barraca.id = :barracaId AND e.produto.id = :produtoId")
    Optional<EstoqueBarraca> findParaVenda(@Param("barracaId") Long barracaId, @Param("produtoId") Long produtoId);
}
