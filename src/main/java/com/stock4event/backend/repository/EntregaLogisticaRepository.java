package com.stock4event.backend.repository;

import com.stock4event.backend.entity.EntregaLogistica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntregaLogisticaRepository extends JpaRepository<EntregaLogistica, Long> {

    @Query("""
            select e from EntregaLogistica e
            join fetch e.caminhoneiro c
            where c.empresa.id = :empresaId
              and e.status <> 'Entregue'
            order by e.id desc
            """)
    List<EntregaLogistica> findAtivasPorEmpresa(@Param("empresaId") Long empresaId);

    @Query("""
            select e from EntregaLogistica e
            join fetch e.caminhoneiro c
            where c.empresa.id = :empresaId
            order by e.id desc
            """)
    List<EntregaLogistica> findRecentesPorEmpresa(@Param("empresaId") Long empresaId);
}
