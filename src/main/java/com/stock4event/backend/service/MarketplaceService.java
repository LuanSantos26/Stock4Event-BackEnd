package com.stock4event.backend.service;

import com.stock4event.backend.dto.FornecedorResponseDTO;
import com.stock4event.backend.dto.ProdutoRecenteDTO;
import com.stock4event.backend.dto.ProdutoResponseDTO;
import com.stock4event.backend.dto.QuiosqueDisponivelDTO;
import com.stock4event.backend.entity.Barraca;
import com.stock4event.backend.entity.Empresa;
import com.stock4event.backend.exception.CadastroException;
import com.stock4event.backend.repository.BarracaRepository;
import com.stock4event.backend.repository.EmpresaRepository;
import com.stock4event.backend.repository.EstoqueBarracaRepository;
import com.stock4event.backend.repository.ProdutoRepository;
import com.stock4event.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class MarketplaceService {

    private static final List<String> TIPOS_FORNECEDOR = List.of("DISTRIBUIDOR", "AMBOS");
    /** Teto de segurança para uma única resposta da vitrine. */
    private static final int MAXIMO_VITRINE = 5000;

    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private BarracaRepository barracaRepository;
    @Autowired private EstoqueBarracaRepository estoqueBarracaRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    /** Empresa do usuário logado (pelo id gravado pelo JwtAuthFilter), ou null sem login. */
    public Long empresaDoUsuario(Long usuarioId) {
        if (usuarioId == null) return null;
        return usuarioRepository.findById(usuarioId)
                .map(u -> u.getEmpresa() != null ? u.getEmpresa().getId() : null)
                .orElse(null);
    }

    /** Produtos das distribuidoras, dos cadastrados mais recentemente. Os da própria empresa vêm marcados. */
    @Transactional(readOnly = true)
    public List<ProdutoRecenteDTO> listarProdutosRecentes(int limite, Long empresaUsuarioId) {
        // limite <= 0: todos os produtos publicados (o app usa isso para a vitrine completa).
        int tamanho = limite <= 0 ? MAXIMO_VITRINE : Math.min(limite, MAXIMO_VITRINE);
        return produtoRepository
                .findByEmpresa_TipoInAndAtivoOrderByIdDesc(TIPOS_FORNECEDOR, 1, PageRequest.of(0, tamanho))
                .stream()
                .map(p -> new ProdutoRecenteDTO(p, empresaUsuarioId))
                .toList();
    }

    /** Quiosques ativos das outras empresas, com o estoque de cada um. Filtra por nome do quiosque, evento ou empresa. */
    @Transactional(readOnly = true)
    public List<QuiosqueDisponivelDTO> listarQuiosquesDisponiveis(String busca, Long empresaUsuarioId) {
        Long excluirId = empresaUsuarioId != null ? empresaUsuarioId : -1L;
        String termo = busca == null ? "" : busca.trim().toLowerCase(Locale.ROOT);

        return barracaRepository.findByAtivaAndEvento_Empresa_IdNotOrderByIdDesc(1, excluirId).stream()
                .filter(b -> termo.isEmpty() || correspondeBusca(b, termo))
                .map(b -> new QuiosqueDisponivelDTO(
                        b,
                        estoqueBarracaRepository.findByBarracaId(b.getId()),
                        TIPOS_FORNECEDOR.contains(b.getEvento().getEmpresa().getTipo())))
                .toList();
    }

    private boolean correspondeBusca(Barraca barraca, String termo) {
        return contem(barraca.getNome(), termo)
                || contem(barraca.getEvento().getNome(), termo)
                || contem(barraca.getEvento().getEmpresa().getNome(), termo);
    }

    private boolean contem(String texto, String termo) {
        return texto != null && texto.toLowerCase(Locale.ROOT).contains(termo);
    }

    public List<FornecedorResponseDTO> listarFornecedores(Long empresaCompradoraId) {
        Long excluirId = empresaCompradoraId != null ? empresaCompradoraId : -1L;
        return empresaRepository.findByTipoInAndIdNot(TIPOS_FORNECEDOR, excluirId).stream()
                .map(this::toFornecedorDTO)
                .toList();
    }

    public List<ProdutoResponseDTO> listarProdutosFornecedor(Long fornecedorId) {
        Empresa fornecedor = empresaRepository.findById(fornecedorId)
                .orElseThrow(() -> new CadastroException(HttpStatus.NOT_FOUND, "Fornecedor não encontrado."));

        if (!TIPOS_FORNECEDOR.contains(fornecedor.getTipo())) {
            throw new CadastroException(HttpStatus.BAD_REQUEST, "Empresa informada não é fornecedora.");
        }

        return produtoRepository.findByEmpresaIdAndAtivo(fornecedorId, 1).stream()
                .map(ProdutoResponseDTO::new)
                .toList();
    }

    private FornecedorResponseDTO toFornecedorDTO(Empresa empresa) {
        int total = produtoRepository.findByEmpresaIdAndAtivo(empresa.getId(), 1).size();
        return new FornecedorResponseDTO(empresa, total);
    }
}
