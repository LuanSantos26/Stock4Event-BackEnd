package com.stock4event.backend.dto;

import com.stock4event.backend.entity.Empresa;
import com.stock4event.backend.entity.Perfil;
import com.stock4event.backend.entity.Usuario;
import lombok.Getter;

import java.time.LocalDateTime;


@Getter
public class UsuarioResponseDTO {

    private final Long          id;
    private final String        nome;
    private final String        email;
    private final Perfil        perfil;
    private final Empresa       empresa;
    private final Integer       ativo;
    private final LocalDateTime criadoEm;
    private final String        perfilUso;
    private final String        cpf;

    public UsuarioResponseDTO(Usuario u) {
        this.id        = u.getId();
        this.nome      = u.getNome();
        this.email     = u.getEmail();
        this.perfil    = u.getPerfil();
        this.empresa   = u.getEmpresa();
        this.ativo     = u.getAtivo();
        this.criadoEm  = u.getCriadoEm();
        this.perfilUso = u.getPerfilUso() == null || u.getPerfilUso().isBlank()
                ? "Fornecedor"
                : u.getPerfilUso();
        this.cpf       = u.getCpf();
    }
}
