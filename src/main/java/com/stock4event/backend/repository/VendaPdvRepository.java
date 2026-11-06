package com.stock4event.backend.repository;

import com.stock4event.backend.entity.VendaPdv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendaPdvRepository extends JpaRepository<VendaPdv, Long> {

    Optional<VendaPdv> findByChaveVenda(String chaveVenda);

    Optional<VendaPdv> findByPedidoId(Long pedidoId);
}
