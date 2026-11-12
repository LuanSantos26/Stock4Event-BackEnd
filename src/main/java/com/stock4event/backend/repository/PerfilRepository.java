package com.stock4event.backend.repository;

import com.stock4event.backend.entity.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PerfilRepository extends JpaRepository<Perfil, Long> {
    Optional<Perfil> findByNomeIgnoreCase(String nome);
}
