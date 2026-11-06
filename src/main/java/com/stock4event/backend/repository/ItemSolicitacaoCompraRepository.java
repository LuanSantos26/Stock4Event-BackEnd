package com.stock4event.backend.repository;

import com.stock4event.backend.entity.ItemSolicitacaoCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemSolicitacaoCompraRepository extends JpaRepository<ItemSolicitacaoCompra, Long> {
    List<ItemSolicitacaoCompra> findBySolicitacaoId(Long solicitacaoId);
}
