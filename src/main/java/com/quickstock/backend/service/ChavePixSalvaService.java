package com.quickstock.backend.service;

import com.quickstock.backend.dto.ChavePixSalvaDTO;
import com.quickstock.backend.dto.ChavePixSalvaRequestDTO;
import com.quickstock.backend.entity.ChavePixSalva;
import com.quickstock.backend.entity.Empresa;
import com.quickstock.backend.exception.CadastroException;
import com.quickstock.backend.repository.ChavePixSalvaRepository;
import com.quickstock.backend.repository.EmpresaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChavePixSalvaService {

    private static final List<String> TIPOS_VALIDOS = List.of("cnpj", "cpf", "email", "telefone", "aleatoria");

    @Autowired private ChavePixSalvaRepository chavePixRepository;
    @Autowired private EmpresaRepository empresaRepository;

    public List<ChavePixSalvaDTO> listarPorEmpresa(Long empresaId) {
        validarEmpresa(empresaId);
        return chavePixRepository.findByEmpresaIdOrderByIdDesc(empresaId).stream()
                .map(ChavePixSalvaDTO::new)
                .toList();
    }

    @Transactional
    public ChavePixSalvaDTO criar(ChavePixSalvaRequestDTO dto) {
        Empresa empresa = validarEmpresa(dto.getEmpresaId());

        String tipoChave = dto.getTipoChave().trim().toLowerCase();
        if (!TIPOS_VALIDOS.contains(tipoChave)) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Tipo de chave PIX inválido.");
        }

        String apelido = dto.getApelido().trim();
        if (apelido.isBlank()) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Apelido da chave PIX é obrigatório.");
        }

        String chaveMascarada = dto.getChaveMascarada().trim();
        if (chaveMascarada.isBlank()) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Chave PIX é obrigatória.");
        }

        ChavePixSalva chave = new ChavePixSalva();
        chave.setEmpresa(empresa);
        chave.setApelido(apelido);
        chave.setTipoChave(tipoChave);
        chave.setChaveMascarada(chaveMascarada);
        chave.setUltimosDigitos(dto.getUltimosDigitos().trim());

        return new ChavePixSalvaDTO(chavePixRepository.save(chave));
    }

    @Transactional
    public void remover(Long id, Long empresaId) {
        ChavePixSalva chave = chavePixRepository.findById(id)
                .orElseThrow(() -> new CadastroException(HttpStatus.NOT_FOUND, "Chave PIX não encontrada."));

        if (!chave.getEmpresa().getId().equals(empresaId)) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Chave PIX não pertence à empresa informada.");
        }

        chavePixRepository.delete(chave);
    }

    private Empresa validarEmpresa(Long empresaId) {
        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new CadastroException(HttpStatus.BAD_REQUEST, "Empresa não encontrada."));
    }
}
