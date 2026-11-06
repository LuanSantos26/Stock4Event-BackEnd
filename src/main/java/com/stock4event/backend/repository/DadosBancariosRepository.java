package com.stock4event.backend.repository;

import com.stock4event.backend.entity.DadosBancarios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DadosBancariosRepository extends JpaRepository<DadosBancarios, Long> {
    Optional<DadosBancarios> findByEmpresaId(Long empresaId);
}
