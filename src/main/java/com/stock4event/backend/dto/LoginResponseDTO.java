package com.stock4event.backend.dto;

import com.stock4event.backend.entity.Usuario;
import lombok.Getter;

@Getter
public class LoginResponseDTO {

    private final String token;
    private final long expiresIn;
    private final UsuarioResponseDTO usuario;

    public LoginResponseDTO(String token, long expiresIn, Usuario usuario) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.usuario = new UsuarioResponseDTO(usuario);
    }
}
