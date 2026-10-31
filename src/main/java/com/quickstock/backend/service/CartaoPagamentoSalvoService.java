package com.quickstock.backend.service;

import com.quickstock.backend.dto.CartaoPagamentoSalvoDTO;
import com.quickstock.backend.dto.CartaoPagamentoSalvoRequestDTO;
import com.quickstock.backend.entity.CartaoPagamentoSalvo;
import com.quickstock.backend.entity.Empresa;
import com.quickstock.backend.exception.CadastroException;
import com.quickstock.backend.repository.CartaoPagamentoSalvoRepository;
import com.quickstock.backend.repository.EmpresaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class CartaoPagamentoSalvoService {

    private static final List<String> TIPOS_VALIDOS = List.of("credito", "debito");
    private static final Pattern VALIDADE_PATTERN = Pattern.compile("^\\d{2}/\\d{2}$");

    @Autowired private CartaoPagamentoSalvoRepository cartaoRepository;
    @Autowired private EmpresaRepository empresaRepository;

    public List<CartaoPagamentoSalvoDTO> listarPorEmpresa(Long empresaId, String tipo) {
        validarEmpresa(empresaId);
        List<CartaoPagamentoSalvo> cartoes = tipo != null && !tipo.isBlank()
                ? cartaoRepository.findByEmpresaIdAndTipoOrderByIdDesc(empresaId, tipo.trim().toLowerCase())
                : cartaoRepository.findByEmpresaIdOrderByIdDesc(empresaId);
        return cartoes.stream().map(CartaoPagamentoSalvoDTO::new).toList();
    }

    @Transactional
    public CartaoPagamentoSalvoDTO criar(CartaoPagamentoSalvoRequestDTO dto) {
        Empresa empresa = validarEmpresa(dto.getEmpresaId());

        String tipo = dto.getTipo().trim().toLowerCase();
        if (!TIPOS_VALIDOS.contains(tipo)) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Tipo de cartão inválido.");
        }

        String ultimosDigitos = dto.getUltimosDigitos().replaceAll("\\D", "");
        if (ultimosDigitos.length() != 4) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Últimos 4 dígitos do cartão são obrigatórios.");
        }

        String validade = dto.getValidade().trim();
        if (!VALIDADE_PATTERN.matcher(validade).matches()) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Validade inválida. Use MM/AA.");
        }

        String titular = dto.getTitular().trim();
        if (titular.isBlank()) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Nome do titular é obrigatório.");
        }

        String apelido = dto.getApelido() != null ? dto.getApelido().trim() : null;
        if (apelido != null && apelido.isBlank()) {
            apelido = null;
        }

        CartaoPagamentoSalvo cartao = new CartaoPagamentoSalvo();
        cartao.setEmpresa(empresa);
        cartao.setTipo(tipo);
        cartao.setApelido(apelido);
        cartao.setBandeira(dto.getBandeira().trim());
        cartao.setUltimosDigitos(ultimosDigitos);
        cartao.setNumeroMascarado("•••• •••• •••• " + ultimosDigitos);
        cartao.setValidade(validade);
        cartao.setTitular(titular);

        return new CartaoPagamentoSalvoDTO(cartaoRepository.save(cartao));
    }

    @Transactional
    public void remover(Long id, Long empresaId) {
        CartaoPagamentoSalvo cartao = cartaoRepository.findById(id)
                .orElseThrow(() -> new CadastroException(HttpStatus.NOT_FOUND, "Cartão não encontrado."));

        if (!cartao.getEmpresa().getId().equals(empresaId)) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Cartão não pertence à empresa informada.");
        }

        cartaoRepository.delete(cartao);
    }

    private Empresa validarEmpresa(Long empresaId) {
        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new CadastroException(HttpStatus.BAD_REQUEST, "Empresa não encontrada."));
    }
}
