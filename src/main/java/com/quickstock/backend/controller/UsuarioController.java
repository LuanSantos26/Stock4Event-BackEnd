package com.quickstock.backend.controller;

import com.quickstock.backend.dto.LoginResponseDTO;
import com.quickstock.backend.dto.UsuarioResponseDTO;
import com.quickstock.backend.entity.Usuario;
import com.quickstock.backend.repository.EmpresaRepository;
import com.quickstock.backend.repository.PerfilRepository;
import com.quickstock.backend.repository.UsuarioRepository;
import com.quickstock.backend.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PerfilRepository  perfilRepository;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private JwtService jwtService;

    @GetMapping("/me")
    public ResponseEntity<?> usuarioAtual(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractBearerToken(authorization);
        if (token == null || !jwtService.isTokenValid(token)) {
            return ResponseEntity.status(401).body(Map.of("erro", "Token inválido ou expirado."));
        }

        return usuarioRepository.findById(jwtService.getUserId(token))
                .filter(u -> u.getAtivo() == 1)
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(new UsuarioResponseDTO(u)))
                .orElse(ResponseEntity.status(401).body(Map.of("erro", "Usuário não encontrado.")));
    }

    @GetMapping
    public List<UsuarioResponseDTO> listar() {
        return usuarioRepository.findAll()
                .stream()
                .map(UsuarioResponseDTO::new)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscar(@PathVariable Long id) {
        return usuarioRepository.findById(id)
                .map(u -> ResponseEntity.ok(new UsuarioResponseDTO(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/empresa/{empresaId}")
    public List<UsuarioResponseDTO> listarPorEmpresa(@PathVariable Long empresaId) {
        return usuarioRepository.findByEmpresaId(empresaId)
                .stream()
                .map(UsuarioResponseDTO::new)
                .toList();
    }

    @PostMapping("/register")
    public ResponseEntity<?> registrar(@Valid @RequestBody Map<String, Object> body) {
        if (usuarioRepository.existsByEmail((String) body.get("email"))) {
            return ResponseEntity.status(409).body(Map.of("erro", "E-mail já cadastrado."));
        }

        var perfil  = perfilRepository.findById(Long.parseLong(body.get("perfilId").toString()));
        var empresa = empresaRepository.findById(Long.parseLong(body.get("empresaId").toString()));

        if (perfil.isEmpty())  return ResponseEntity.badRequest().body(Map.of("erro", "Perfil não encontrado."));
        if (empresa.isEmpty()) return ResponseEntity.badRequest().body(Map.of("erro", "Empresa não encontrada."));

        String hash = org.springframework.security.crypto.bcrypt.BCrypt.hashpw(
                (String) body.get("senha"),
                org.springframework.security.crypto.bcrypt.BCrypt.gensalt()
        );

        Usuario u = new Usuario();
        u.setNome((String) body.get("nome"));
        u.setEmail((String) body.get("email"));
        u.setSenhaHash(hash);
        u.setPerfil(perfil.get());
        u.setEmpresa(empresa.get());
        u.setAtivo(1);

        return ResponseEntity.status(201).body(new UsuarioResponseDTO(usuarioRepository.save(u)));
    }

    @PostMapping("/recuperar-senha")
    public ResponseEntity<?> recuperarSenha(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String novaSenha = body.get("novaSenha");

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Informe o e-mail."));
        }
        if (novaSenha == null || novaSenha.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("erro", "A nova senha deve ter pelo menos 6 caracteres."));
        }

        return usuarioRepository.findByEmailIgnoreCase(email.trim())
                .map(u -> {
                    String hash = org.springframework.security.crypto.bcrypt.BCrypt.hashpw(
                            novaSenha,
                            org.springframework.security.crypto.bcrypt.BCrypt.gensalt()
                    );
                    u.setSenhaHash(hash);
                    usuarioRepository.save(u);
                    return ResponseEntity.ok(Map.of("mensagem", "Senha atualizada."));
                })
                .orElse(ResponseEntity.status(404).body(Map.of("erro", "E-mail não encontrado.")));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String senha = body.get("senha");

        if (email == null || email.isBlank() || senha == null) {
            return ResponseEntity.status(401).body(Map.of("erro", "E-mail ou senha incorretos."));
        }

        return usuarioRepository.findByEmailIgnoreCase(email.trim())
                .filter(u -> u.getAtivo() == 1)
                .filter(u -> org.springframework.security.crypto.bcrypt.BCrypt.checkpw(senha, u.getSenhaHash()))
                .map(u -> {
                    String token = jwtService.generateToken(u);
                    return ResponseEntity.ok((Object) new LoginResponseDTO(
                            token,
                            jwtService.getExpirationMs(),
                            u
                    ));
                })
                .orElse(ResponseEntity.status(401).body(Map.of("erro", "E-mail ou senha incorretos.")));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id,
                                       @RequestBody Map<String, Object> body) {
        return usuarioRepository.findById(id).map(u -> {
            if (body.containsKey("nome"))    u.setNome((String) body.get("nome"));
            if (body.containsKey("email"))   u.setEmail((String) body.get("email"));
            if (body.containsKey("ativo"))   u.setAtivo((Integer) body.get("ativo"));
            if (body.containsKey("perfilId")) {
                perfilRepository.findById(Long.parseLong(body.get("perfilId").toString()))
                        .ifPresent(u::setPerfil);
            }
            if (body.containsKey("senha")) {
                String hash = org.springframework.security.crypto.bcrypt.BCrypt.hashpw(
                        (String) body.get("senha"),
                        org.springframework.security.crypto.bcrypt.BCrypt.gensalt()
                );
                u.setSenhaHash(hash);
            }
            return ResponseEntity.ok((Object) new UsuarioResponseDTO(usuarioRepository.save(u)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        if (!usuarioRepository.existsById(id)) return ResponseEntity.notFound().build();
        usuarioRepository.findById(id).ifPresent(u -> {
            u.setAtivo(0);
            usuarioRepository.save(u);
        });
        return ResponseEntity.noContent().build();
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7).trim();
    }
}
