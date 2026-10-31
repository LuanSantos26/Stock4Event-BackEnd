package com.quickstock.backend.repository;

import com.quickstock.backend.entity.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    List<Pagamento> findByPedidoId(Long pedidoId);

    @Query("""
            SELECT pg FROM Pagamento pg
            WHERE pg.pedido.barraca.evento.empresa.id = :empresaId
            AND pg.pedido.criadoEm >= :desde AND pg.pedido.criadoEm < :ate
            AND (pg.pedido.status IS NULL OR LOWER(pg.pedido.status) <> 'cancelado')
            """)
    List<Pagamento> findPorEmpresaNoPeriodo(
            @Param("empresaId") Long empresaId,
            @Param("desde") LocalDateTime desde,
            @Param("ate") LocalDateTime ate);
}
