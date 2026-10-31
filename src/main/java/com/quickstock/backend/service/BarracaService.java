package com.quickstock.backend.service;

import com.quickstock.backend.dto.BarracaRequestDTO;
import com.quickstock.backend.dto.BarracaResponseDTO;
import com.quickstock.backend.dto.EstoqueItemRequestDTO;
import com.quickstock.backend.entity.Barraca;
import com.quickstock.backend.entity.Empresa;
import com.quickstock.backend.entity.EstoqueBarraca;
import com.quickstock.backend.entity.Evento;
import com.quickstock.backend.entity.Produto;
import com.quickstock.backend.entity.Usuario;
import com.quickstock.backend.exception.CadastroException;
import com.quickstock.backend.repository.BarracaRepository;
import com.quickstock.backend.repository.EmpresaRepository;
import com.quickstock.backend.repository.EstoqueBarracaRepository;
import com.quickstock.backend.repository.EventoRepository;
import com.quickstock.backend.repository.ProdutoRepository;
import com.quickstock.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class BarracaService {

    private static final String STATUS_ATIVO = "ativo";

    @Autowired private BarracaRepository barracaRepository;
    @Autowired private EstoqueBarracaRepository estoqueRepository;
    @Autowired private EventoRepository eventoRepository;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ProdutoRepository produtoRepository;

    public List<BarracaResponseDTO> listarPorEmpresa(Long empresaId) {
        return barracaRepository.findByEvento_EmpresaIdAndAtiva(empresaId, 1).stream()
                .map(this::toResponse)
                .toList();
    }

    public BarracaResponseDTO buscarPorId(Long id) {
        Barraca barraca = barracaRepository.findById(id)
                .orElseThrow(() -> new CadastroException(HttpStatus.NOT_FOUND, "Barraquinha não encontrada."));
        return toResponse(barraca);
    }

    @Transactional
    public BarracaResponseDTO criar(BarracaRequestDTO dto) {
        Empresa empresa = empresaRepository.findById(dto.getEmpresaId())
                .orElseThrow(() -> new CadastroException(HttpStatus.BAD_REQUEST, "Empresa não encontrada."));

        Usuario responsavel = usuarioRepository.findById(dto.getResponsavelId())
                .orElseThrow(() -> new CadastroException(HttpStatus.BAD_REQUEST, "Responsável não encontrado."));

        if (!responsavel.getEmpresa().getId().equals(empresa.getId())) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Responsável não pertence à empresa informada.");
        }

        Evento evento = obterOuCriarEventoPadrao(empresa);

        Barraca barraca = new Barraca();
        barraca.setNome(dto.getNome().trim());
        barraca.setEvento(evento);
        barraca.setResponsavel(responsavel);
        barraca.setAtiva(1);
        barraca = barracaRepository.save(barraca);

        sincronizarEstoque(barraca, dto.getItens());

        return toResponse(barraca);
    }

    @Transactional
    public BarracaResponseDTO atualizar(Long id, BarracaRequestDTO dto) {
        Barraca barraca = barracaRepository.findById(id)
                .orElseThrow(() -> new CadastroException(HttpStatus.NOT_FOUND, "Barraquinha não encontrada."));

        validarPertenceEmpresa(barraca, dto.getEmpresaId());

        barraca.setNome(dto.getNome().trim());
        barraca = barracaRepository.save(barraca);

        if (dto.getItens() != null && !dto.getItens().isEmpty()) {
            sincronizarEstoque(barraca, dto.getItens());
        }

        return toResponse(barraca);
    }

    @Transactional
    public BarracaResponseDTO sincronizarEstoque(Long barracaId, List<EstoqueItemRequestDTO> itens) {
        Barraca barraca = barracaRepository.findById(barracaId)
                .orElseThrow(() -> new CadastroException(HttpStatus.NOT_FOUND, "Barraquinha não encontrada."));

        sincronizarEstoque(barraca, itens);
        return toResponse(barraca);
    }

    @Transactional
    public void desativar(Long id, Long empresaId) {
        Barraca barraca = barracaRepository.findById(id)
                .orElseThrow(() -> new CadastroException(HttpStatus.NOT_FOUND, "Barraquinha não encontrada."));

        validarPertenceEmpresa(barraca, empresaId);
        barraca.setAtiva(0);
        barracaRepository.save(barraca);
    }

    private Evento obterOuCriarEventoPadrao(Empresa empresa) {
        return eventoRepository
                .findFirstByEmpresaIdAndStatusOrderByDataInicioDesc(empresa.getId(), STATUS_ATIVO)
                .orElseGet(() -> {
                    Evento evento = new Evento();
                    evento.setEmpresa(empresa);
                    evento.setNome("Operação principal");
                    evento.setDataInicio(LocalDate.now());
                    evento.setDataFim(LocalDate.now().plusYears(1));
                    evento.setStatus(STATUS_ATIVO);
                    return eventoRepository.save(evento);
                });
    }

    private void sincronizarEstoque(Barraca barraca, List<EstoqueItemRequestDTO> itens) {
        if (itens == null || itens.isEmpty()) {
            return;
        }

        Long empresaId = barraca.getEvento().getEmpresa().getId();

        for (EstoqueItemRequestDTO item : itens) {
            if (item.getQuantidade() == null) {
                continue;
            }

            Produto produto = produtoRepository.findById(item.getProdutoId())
                    .orElseThrow(() -> new CadastroException(
                            HttpStatus.BAD_REQUEST,
                            "Produto não encontrado: " + item.getProdutoId()
                    ));

            if (!produto.getEmpresa().getId().equals(empresaId)) {
                throw new CadastroException(HttpStatus.BAD_REQUEST, "Produto não pertence à empresa.");
            }

            EstoqueBarraca estoque = estoqueRepository
                    .findByBarracaIdAndProdutoId(barraca.getId(), produto.getId())
                    .orElseGet(() -> {
                        EstoqueBarraca novo = new EstoqueBarraca();
                        novo.setBarraca(barraca);
                        novo.setProduto(produto);
                        novo.setQuantidade(BigDecimal.ZERO);
                        return novo;
                    });

            estoque.setQuantidade(item.getQuantidade());
            estoqueRepository.save(estoque);
        }
    }

    private void validarPertenceEmpresa(Barraca barraca, Long empresaId) {
        if (!barraca.getEvento().getEmpresa().getId().equals(empresaId)) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Barraquinha não pertence à empresa informada.");
        }
    }

    private BarracaResponseDTO toResponse(Barraca barraca) {
        List<EstoqueBarraca> itens = estoqueRepository.findByBarracaId(barraca.getId());
        return new BarracaResponseDTO(barraca, itens);
    }
}
