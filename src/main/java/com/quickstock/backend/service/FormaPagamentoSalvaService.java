package com.quickstock.backend.service;

import com.quickstock.backend.dto.FormaPagamentoSalvaDTO;
import com.quickstock.backend.dto.FormaPagamentoSalvaRequestDTO;
import com.quickstock.backend.entity.Empresa;
import com.quickstock.backend.entity.FormaPagamentoSalva;
import com.quickstock.backend.exception.CadastroException;
import com.quickstock.backend.repository.EmpresaRepository;
import com.quickstock.backend.repository.FormaPagamentoSalvaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FormaPagamentoSalvaService {

    private static final List<String> TIPOS_VALIDOS = List.of("pix", "credito", "debito", "dinheiro");

    @Autowired private FormaPagamentoSalvaRepository formaRepository;
    @Autowired private EmpresaRepository empresaRepository;

    public List<FormaPagamentoSalvaDTO> listarPorEmpresa(Long empresaId) {
        return formaRepository.findByEmpresaIdOrderByPrincipalDescIdAsc(empresaId).stream()
                .map(FormaPagamentoSalvaDTO::new)
                .toList();
    }

    @Transactional
    public FormaPagamentoSalvaDTO criar(FormaPagamentoSalvaRequestDTO dto) {
        Empresa empresa = empresaRepository.findById(dto.getEmpresaId())
                .orElseThrow(() -> new CadastroException(HttpStatus.BAD_REQUEST, "Empresa não encontrada."));

        String tipo = dto.getTipo().trim().toLowerCase();
        if (!TIPOS_VALIDOS.contains(tipo)) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Tipo de pagamento inválido.");
        }

        String apelido = dto.getApelido().trim();
        if (apelido.isBlank()) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Apelido é obrigatório.");
        }

        if (formaRepository.existsByEmpresaIdAndTipoIgnoreCaseAndApelidoIgnoreCase(empresa.getId(), tipo, apelido)) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Já existe uma forma de pagamento com este tipo e apelido.");
        }

        long total = formaRepository.countByEmpresaId(empresa.getId());
        boolean principal = dto.isPrincipal() || total == 0;

        if (principal && total > 0) {
            formaRepository.findByEmpresaIdOrderByPrincipalDescIdAsc(empresa.getId())
                    .forEach(f -> {
                        f.setPrincipal(false);
                        formaRepository.save(f);
                    });
        }

        FormaPagamentoSalva forma = new FormaPagamentoSalva();
        forma.setEmpresa(empresa);
        forma.setTipo(tipo);
        forma.setApelido(apelido);
        forma.setPrincipal(principal);

        return new FormaPagamentoSalvaDTO(formaRepository.save(forma));
    }

    @Transactional
    public FormaPagamentoSalvaDTO definirPrincipal(Long id, Long empresaId) {
        FormaPagamentoSalva escolhida = buscarDaEmpresa(id, empresaId);
        for (FormaPagamentoSalva f : formaRepository.findByEmpresaIdOrderByPrincipalDescIdAsc(empresaId)) {
            boolean principal = f.getId().equals(escolhida.getId());
            if (principal != f.isPrincipal()) {
                f.setPrincipal(principal);
                formaRepository.save(f);
            }
        }
        escolhida.setPrincipal(true);
        return new FormaPagamentoSalvaDTO(escolhida);
    }

    @Transactional
    public void remover(Long id, Long empresaId) {
        FormaPagamentoSalva forma = buscarDaEmpresa(id, empresaId);
        boolean eraPrincipal = forma.isPrincipal();

        formaRepository.delete(forma);

        if (eraPrincipal) {
            formaRepository.findByEmpresaIdOrderByPrincipalDescIdAsc(empresaId).stream()
                    .findFirst()
                    .ifPresent(f -> {
                        f.setPrincipal(true);
                        formaRepository.save(f);
                    });
        }
    }

    private FormaPagamentoSalva buscarDaEmpresa(Long id, Long empresaId) {
        FormaPagamentoSalva forma = formaRepository.findById(id)
                .orElseThrow(() -> new CadastroException(HttpStatus.NOT_FOUND, "Forma de pagamento não encontrada."));

        if (!forma.getEmpresa().getId().equals(empresaId)) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Forma de pagamento não pertence à empresa informada.");
        }
        return forma;
    }
}
