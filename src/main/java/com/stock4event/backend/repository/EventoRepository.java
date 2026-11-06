package com.stock4event.backend.repository;

import com.stock4event.backend.entity.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Long> {
    List<Evento> findByEmpresaId(Long empresaId);
    List<Evento> findByEmpresaIdAndStatus(Long empresaId, String status);
    java.util.Optional<Evento> findFirstByEmpresaIdAndStatusOrderByDataInicioDesc(Long empresaId, String status);
}
