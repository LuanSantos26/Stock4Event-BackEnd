package com.quickstock.backend.service;

import com.quickstock.backend.dto.CadastroContaRequestDTO;
import com.quickstock.backend.entity.Empresa;
import com.quickstock.backend.entity.Perfil;
import com.quickstock.backend.entity.Usuario;
import com.quickstock.backend.exception.CadastroException;
import com.quickstock.backend.repository.EmpresaRepository;
import com.quickstock.backend.repository.PerfilRepository;
import com.quickstock.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class CadastroService {

    private static final List<String> PERFIS_ADMIN = List.of("Admin", "Gestor", "Gerente");
    private static final Set<String> PERFIS_USO = Set.of("Cliente", "Fornecedor", "ClienteFornecedor");

    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PerfilRepository perfilRepository;

    @Transactional
    public Usuario cadastrarConta(CadastroContaRequestDTO request) {
        var usuarioDto = request.getUsuario();
        var empresaDto = request.getEmpresa();

        String email = usuarioDto.getEmail().trim().toLowerCase();
        String cnpj = empresaDto.getCnpj().trim();
        String perfilUso = normalizarPerfil(request.getPerfil());

        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new CadastroException(HttpStatus.CONFLICT, "E-mail já cadastrado.");
        }

        if (empresaRepository.existsByCnpj(cnpj)) {
            String mensagem = somenteDigitos(cnpj).length() == 11
                    ? "CPF já cadastrado."
                    : "CNPJ já cadastrado.";
            throw new CadastroException(HttpStatus.CONFLICT, mensagem);
        }

        Perfil perfilAdmin = buscarPerfilAdmin()
                .orElseThrow(() -> new CadastroException(
                        HttpStatus.BAD_REQUEST,
                        "Perfil de administrador não encontrado no sistema."
                ));

        Empresa empresa = new Empresa();
        empresa.setNome(empresaDto.getNome().trim());
        empresa.setCnpj(cnpj);
        if (empresaDto.getTelefone() != null && !empresaDto.getTelefone().isBlank()) {
            empresa.setTelefone(empresaDto.getTelefone().trim());
        }
        empresa.setTipo(tipoEmpresa(perfilUso));
        empresa = empresaRepository.save(empresa);

        String hash = BCrypt.hashpw(usuarioDto.getSenha(), BCrypt.gensalt());

        Usuario usuario = new Usuario();
        usuario.setNome(usuarioDto.getNome().trim());
        usuario.setEmail(email);
        usuario.setSenhaHash(hash);
        usuario.setPerfil(perfilAdmin);
        usuario.setEmpresa(empresa);
        usuario.setAtivo(1);
        usuario.setPerfilUso(perfilUso);
        if (usuarioDto.getCpf() != null && !usuarioDto.getCpf().isBlank()) {
            usuario.setCpf(somenteDigitos(usuarioDto.getCpf()));
        }

        return usuarioRepository.save(usuario);
    }

    private String normalizarPerfil(String perfil) {
        if (perfil == null || perfil.isBlank()) {
            return "Fornecedor";
        }
        String valor = perfil.trim();
        if (!PERFIS_USO.contains(valor)) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Perfil de cadastro inválido.");
        }
        return valor;
    }

    private String tipoEmpresa(String perfilUso) {
        return switch (perfilUso) {
            case "Cliente" -> "CLIENTE";
            case "ClienteFornecedor" -> "AMBOS";
            default -> "DISTRIBUIDOR";
        };
    }

    private String somenteDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }

    private java.util.Optional<Perfil> buscarPerfilAdmin() {
        for (String nome : PERFIS_ADMIN) {
            var perfil = perfilRepository.findByNomeIgnoreCase(nome);
            if (perfil.isPresent()) {
                return perfil;
            }
        }
        return java.util.Optional.empty();
    }
}
