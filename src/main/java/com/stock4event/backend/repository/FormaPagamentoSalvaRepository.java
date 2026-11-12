package com.stock4event.backend.repository;

import com.stock4event.backend.entity.FormaPagamentoSalva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FormaPagamentoSalvaRepository extends JpaRepository<FormaPagamentoSalva, Long> {

    List<FormaPagamentoSalva> findByEmpresaIdOrderByPrincipalDescIdAsc(Long empresaId);

    long countByEmpresaId(Long empresaId);

    boolean existsByEmpresaIdAndTipoIgnoreCaseAndApelidoIgnoreCase(Long empresaId, String tipo, String apelido);
}
