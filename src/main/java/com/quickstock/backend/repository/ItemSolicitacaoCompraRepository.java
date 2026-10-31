package com.quickstock.backend.repository;

import com.quickstock.backend.entity.ItemSolicitacaoCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemSolicitacaoCompraRepository extends JpaRepository<ItemSolicitacaoCompra, Long> {
    List<ItemSolicitacaoCompra> findBySolicitacaoId(Long solicitacaoId);
}
