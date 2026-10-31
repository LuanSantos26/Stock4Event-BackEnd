package com.quickstock.backend.repository;

import com.quickstock.backend.entity.EnderecoEntrega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnderecoEntregaRepository extends JpaRepository<EnderecoEntrega, Long> {
    List<EnderecoEntrega> findByEmpresaIdOrderByPrincipalDescApelidoAsc(Long empresaId);
    long countByEmpresaId(Long empresaId);
}
