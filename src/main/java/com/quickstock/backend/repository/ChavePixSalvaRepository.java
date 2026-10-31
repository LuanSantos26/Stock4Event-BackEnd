package com.quickstock.backend.repository;

import com.quickstock.backend.entity.ChavePixSalva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChavePixSalvaRepository extends JpaRepository<ChavePixSalva, Long> {
    List<ChavePixSalva> findByEmpresaIdOrderByIdDesc(Long empresaId);
    long countByEmpresaId(Long empresaId);
}
