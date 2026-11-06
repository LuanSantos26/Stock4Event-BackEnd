package com.stock4event.backend.repository;

import com.stock4event.backend.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    boolean existsByCnpj(String cnpj);
    Optional<Empresa> findByCnpj(String cnpj);
    List<Empresa> findByTipoInAndIdNot(List<String> tipos, Long id);
}
