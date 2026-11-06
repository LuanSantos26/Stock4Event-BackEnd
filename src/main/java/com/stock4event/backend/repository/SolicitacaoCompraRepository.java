package com.stock4event.backend.repository;

import com.stock4event.backend.entity.SolicitacaoCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SolicitacaoCompraRepository extends JpaRepository<SolicitacaoCompra, Long> {
    List<SolicitacaoCompra> findByEmpresaCompradoraIdOrderByCriadoEmDesc(Long empresaCompradoraId);
    List<SolicitacaoCompra> findByEmpresaFornecedoraIdOrderByCriadoEmDesc(Long empresaFornecedoraId);

    @Query("""
            SELECT s FROM SolicitacaoCompra s
            WHERE s.empresaCompradora.id = :empresaId
            AND s.criadoEm >= :desde AND s.criadoEm < :ate
            AND (s.observacao IS NULL OR s.observacao <> :seedObs)
            ORDER BY s.criadoEm DESC
            """)
    List<SolicitacaoCompra> findComprasPorEmpresaNoPeriodo(
            @Param("empresaId") Long empresaId,
            @Param("desde") LocalDateTime desde,
            @Param("ate") LocalDateTime ate,
            @Param("seedObs") String seedObs);
}
