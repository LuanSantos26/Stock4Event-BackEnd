package com.stock4event.backend.repository;

import com.stock4event.backend.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByBarracaId(Long barracaId);
    List<Pedido> findByBarracaIdAndStatus(Long barracaId, String status);
    List<Pedido> findByEnderecoEntregaId(Long enderecoEntregaId);

    @Query("""
            SELECT p FROM Pedido p
            WHERE p.barraca.evento.empresa.id = :empresaId
            AND p.criadoEm >= :desde AND p.criadoEm < :ate
            AND (p.status IS NULL OR LOWER(p.status) <> 'cancelado')
            ORDER BY p.criadoEm DESC
            """)
    List<Pedido> findVendasPorEmpresaNoPeriodo(
            @Param("empresaId") Long empresaId,
            @Param("desde") LocalDateTime desde,
            @Param("ate") LocalDateTime ate);
}
