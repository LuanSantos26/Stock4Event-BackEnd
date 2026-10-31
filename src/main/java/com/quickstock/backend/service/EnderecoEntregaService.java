package com.quickstock.backend.service;

import com.quickstock.backend.dto.EnderecoEntregaDTO;
import com.quickstock.backend.dto.EnderecoEntregaRequestDTO;
import com.quickstock.backend.entity.Empresa;
import com.quickstock.backend.entity.EnderecoEntrega;
import com.quickstock.backend.exception.CadastroException;
import com.quickstock.backend.repository.EmpresaRepository;
import com.quickstock.backend.repository.EnderecoEntregaRepository;
import com.quickstock.backend.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EnderecoEntregaService {

    @Autowired private EnderecoEntregaRepository enderecoRepository;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private PedidoRepository pedidoRepository;

    @Transactional
    public EnderecoEntregaDTO atualizar(Long id, EnderecoEntregaRequestDTO dto) {
        EnderecoEntrega endereco = validarEnderecoDaEmpresa(id, dto.getEmpresaId());

        endereco.setApelido(dto.getApelido().trim());
        endereco.setLogradouro(dto.getLogradouro().trim());
        endereco.setNumero(dto.getNumero().trim());
        endereco.setComplemento(dto.getComplemento() != null ? dto.getComplemento().trim() : null);
        endereco.setBairro(dto.getBairro().trim());
        endereco.setCidade(dto.getCidade().trim());
        endereco.setUf(dto.getUf().trim().toUpperCase());
        endereco.setCep(normalizarCep(dto.getCep()));
        endereco = enderecoRepository.save(endereco);

        if (dto.isPrincipal() && !endereco.isPrincipal()) {
            return definirPrincipal(id, dto.getEmpresaId());
        }
        return new EnderecoEntregaDTO(endereco);
    }

    @Transactional
    public EnderecoEntregaDTO definirPrincipal(Long id, Long empresaId) {
        EnderecoEntrega escolhido = validarEnderecoDaEmpresa(id, empresaId);
        for (EnderecoEntrega e : enderecoRepository.findByEmpresaIdOrderByPrincipalDescApelidoAsc(empresaId)) {
            boolean principal = e.getId().equals(escolhido.getId());
            if (principal != e.isPrincipal()) {
                e.setPrincipal(principal);
                enderecoRepository.save(e);
            }
        }
        escolhido.setPrincipal(true);
        return new EnderecoEntregaDTO(escolhido);
    }

    @Transactional
    public void remover(Long id, Long empresaId) {
        EnderecoEntrega endereco = validarEnderecoDaEmpresa(id, empresaId);
        boolean eraPrincipal = endereco.isPrincipal();

        // Os pedidos guardam uma cópia do endereço; só desfaz o vínculo.
        pedidoRepository.findByEnderecoEntregaId(id).forEach(p -> {
            p.setEnderecoEntrega(null);
            pedidoRepository.save(p);
        });
        enderecoRepository.delete(endereco);

        if (eraPrincipal) {
            enderecoRepository.findByEmpresaIdOrderByPrincipalDescApelidoAsc(empresaId).stream()
                    .findFirst()
                    .ifPresent(e -> {
                        e.setPrincipal(true);
                        enderecoRepository.save(e);
                    });
        }
    }

    public List<EnderecoEntregaDTO> listarPorEmpresa(Long empresaId) {
        return enderecoRepository.findByEmpresaIdOrderByPrincipalDescApelidoAsc(empresaId).stream()
                .map(EnderecoEntregaDTO::new)
                .toList();
    }

    public EnderecoEntrega buscarPorId(Long id) {
        return enderecoRepository.findById(id)
                .orElseThrow(() -> new CadastroException(HttpStatus.BAD_REQUEST, "Endereço de entrega não encontrado."));
    }

    public EnderecoEntrega validarEnderecoDaEmpresa(Long enderecoId, Long empresaId) {
        EnderecoEntrega endereco = buscarPorId(enderecoId);
        if (!endereco.getEmpresa().getId().equals(empresaId)) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Endereço não pertence à empresa compradora.");
        }
        return endereco;
    }

    public EnderecoEntrega salvar(EnderecoEntrega endereco, Long empresaId) {
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new CadastroException(HttpStatus.BAD_REQUEST, "Empresa não encontrada."));
        endereco.setEmpresa(empresa);
        return enderecoRepository.save(endereco);
    }

    @Transactional
    public EnderecoEntregaDTO criar(EnderecoEntregaRequestDTO dto) {
        Empresa empresa = empresaRepository.findById(dto.getEmpresaId())
                .orElseThrow(() -> new CadastroException(HttpStatus.BAD_REQUEST, "Empresa não encontrada."));

        long total = enderecoRepository.countByEmpresaId(empresa.getId());
        boolean principal = dto.isPrincipal() || total == 0;

        if (principal && total > 0) {
            enderecoRepository.findByEmpresaIdOrderByPrincipalDescApelidoAsc(empresa.getId())
                    .forEach(e -> {
                        e.setPrincipal(false);
                        enderecoRepository.save(e);
                    });
        }

        EnderecoEntrega endereco = new EnderecoEntrega();
        endereco.setEmpresa(empresa);
        endereco.setApelido(dto.getApelido().trim());
        endereco.setLogradouro(dto.getLogradouro().trim());
        endereco.setNumero(dto.getNumero().trim());
        endereco.setComplemento(dto.getComplemento() != null ? dto.getComplemento().trim() : null);
        endereco.setBairro(dto.getBairro().trim());
        endereco.setCidade(dto.getCidade().trim());
        endereco.setUf(dto.getUf().trim().toUpperCase());
        endereco.setCep(normalizarCep(dto.getCep()));
        endereco.setPrincipal(principal);

        return new EnderecoEntregaDTO(enderecoRepository.save(endereco));
    }

    private String normalizarCep(String cep) {
        if (cep == null) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "CEP é obrigatório.");
        }
        String digits = cep.replaceAll("\\D", "");
        if (digits.length() != 8) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "CEP inválido. Informe 8 números.");
        }
        return digits.substring(0, 5) + "-" + digits.substring(5);
    }
}
