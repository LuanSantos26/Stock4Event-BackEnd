package com.stock4event.backend.repository;

import com.stock4event.backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByEmailIgnoreCase(String email);
    List<Usuario> findByEmpresaId(Long empresaId);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
}
