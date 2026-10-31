package com.quickstock.backend.repository;

import com.quickstock.backend.entity.Caminhoneiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaminhoneiroRepository extends JpaRepository<Caminhoneiro, Long> {
    List<Caminhoneiro> findByEmpresaIdOrderByIdDesc(Long empresaId);
}
