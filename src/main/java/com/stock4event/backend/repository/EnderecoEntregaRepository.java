package com.stock4event.backend.repository;

import com.stock4event.backend.entity.EnderecoEntrega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnderecoEntregaRepository extends JpaRepository<EnderecoEntrega, Long> {
    List<EnderecoEntrega> findByEmpresaIdOrderByPrincipalDescApelidoAsc(Long empresaId);
    long countByEmpresaId(Long empresaId);
}
