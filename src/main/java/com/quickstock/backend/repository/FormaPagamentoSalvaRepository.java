package com.quickstock.backend.repository;

import com.quickstock.backend.entity.FormaPagamentoSalva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FormaPagamentoSalvaRepository extends JpaRepository<FormaPagamentoSalva, Long> {

    List<FormaPagamentoSalva> findByEmpresaIdOrderByPrincipalDescIdAsc(Long empresaId);

    long countByEmpresaId(Long empresaId);

    boolean existsByEmpresaIdAndTipoIgnoreCaseAndApelidoIgnoreCase(Long empresaId, String tipo, String apelido);
}
