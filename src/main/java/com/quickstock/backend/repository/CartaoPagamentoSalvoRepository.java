package com.quickstock.backend.repository;

import com.quickstock.backend.entity.CartaoPagamentoSalvo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartaoPagamentoSalvoRepository extends JpaRepository<CartaoPagamentoSalvo, Long> {
    List<CartaoPagamentoSalvo> findByEmpresaIdOrderByIdDesc(Long empresaId);
    List<CartaoPagamentoSalvo> findByEmpresaIdAndTipoOrderByIdDesc(Long empresaId, String tipo);
}
