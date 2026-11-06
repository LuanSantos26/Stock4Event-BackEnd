package com.stock4event.backend.service;

import com.stock4event.backend.dto.DadosBancariosDTO;
import com.stock4event.backend.dto.DadosBancariosRequestDTO;
import com.stock4event.backend.entity.DadosBancarios;
import com.stock4event.backend.entity.Empresa;
import com.stock4event.backend.exception.CadastroException;
import com.stock4event.backend.repository.DadosBancariosRepository;
import com.stock4event.backend.repository.EmpresaRepository;
import com.stock4event.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** Conta de recebimento do fornecedor: consulta (qualquer usuário logado) e cadastro (só a própria empresa). */
@Service
public class DadosBancariosService {

    static final Set<String> TIPOS_CONTA = Set.of("corrente", "poupanca");
    static final Set<String> TIPOS_CHAVE = Set.of("cpf", "cnpj", "email", "telefone", "aleatoria");

    @Autowired private DadosBancariosRepository repository;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public Optional<DadosBancariosDTO> buscar(Long empresaId) {
        return repository.findByEmpresaId(empresaId).map(DadosBancariosDTO::new);
    }

    @Transactional
    public DadosBancariosDTO salvar(DadosBancariosRequestDTO dto, Long usuarioId) {
        Empresa empresa = empresaRepository.findById(dto.getEmpresaId())
                .orElseThrow(() -> new CadastroException(HttpStatus.NOT_FOUND, "Empresa não encontrada."));
        validarDono(empresa, usuarioId);

        DadosBancarios dados = repository.findByEmpresaId(empresa.getId()).orElseGet(DadosBancarios::new);
        dados.setEmpresa(empresa);
        dados.setBancoCodigo(validarBanco(dto.getBancoCodigo()));
        dados.setBancoNome(dto.getBancoNome().trim());
        dados.setAgencia(validarAgencia(dto.getAgencia()));
        dados.setConta(validarConta(dto.getConta()));
        dados.setTipoConta(validarOpcao(dto.getTipoConta(), TIPOS_CONTA, "Tipo de conta inválido."));
        dados.setTitular(dto.getTitular().trim());
        dados.setDocumento(validarDocumento(dto.getDocumento()));

        String chave = dto.getChavePix() == null ? "" : dto.getChavePix().trim();
        if (chave.isEmpty()) {
            dados.setTipoChavePix(null);
            dados.setChavePix(null);
        } else {
            dados.setTipoChavePix(validarOpcao(dto.getTipoChavePix(), TIPOS_CHAVE, "Tipo de chave Pix inválido."));
            dados.setChavePix(chave);
        }
        return new DadosBancariosDTO(repository.save(dados));
    }

    private void validarDono(Empresa empresa, Long usuarioId) {
        if (usuarioId == null) return; // autenticação desligada (testes no Swagger)
        Long empresaUsuario = usuarioRepository.findById(usuarioId)
                .map(u -> u.getEmpresa() != null ? u.getEmpresa().getId() : null)
                .orElse(null);
        if (!empresa.getId().equals(empresaUsuario)) {
            throw new CadastroException(HttpStatus.FORBIDDEN, "Você só pode alterar os dados bancários da sua empresa.");
        }
    }

    static String validarBanco(String codigo) {
        String digitos = codigo == null ? "" : codigo.replaceAll("\\D", "");
        if (digitos.isEmpty() || digitos.length() > 3) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Código do banco deve ter até 3 dígitos.");
        }
        return String.format("%03d", Integer.parseInt(digitos));
    }

    static String validarAgencia(String agencia) {
        String valor = agencia == null ? "" : agencia.trim();
        if (!valor.matches("\\d{1,5}(-[\\dXx])?")) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Agência inválida. Use só números (ex.: 1234 ou 1234-5).");
        }
        return valor.toUpperCase(Locale.ROOT);
    }

    static String validarConta(String conta) {
        String valor = conta == null ? "" : conta.trim();
        if (!valor.matches("\\d{1,13}-?[\\dXx]")) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Conta inválida. Use números com o dígito (ex.: 12345-6).");
        }
        return valor.toUpperCase(Locale.ROOT);
    }

    static String validarDocumento(String documento) {
        String digitos = documento == null ? "" : documento.replaceAll("\\D", "");
        if (digitos.length() != 11 && digitos.length() != 14) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "CPF ou CNPJ do titular incompleto.");
        }
        return digitos;
    }

    static String validarOpcao(String valor, Set<String> permitidos, String erro) {
        String v = valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
        if (!permitidos.contains(v)) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, erro);
        }
        return v;
    }
}
