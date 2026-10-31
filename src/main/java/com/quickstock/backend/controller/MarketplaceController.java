package com.quickstock.backend.controller;

import com.quickstock.backend.config.JwtAuthFilter;
import com.quickstock.backend.dto.ProdutoRecenteDTO;
import com.quickstock.backend.dto.ProdutoResponseDTO;
import com.quickstock.backend.dto.FornecedorResponseDTO;
import com.quickstock.backend.dto.QuiosqueDisponivelDTO;
import com.quickstock.backend.service.MarketplaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/marketplace")
@CrossOrigin(origins = "*")
public class MarketplaceController {

    @Autowired private MarketplaceService marketplaceService;

    @GetMapping("/fornecedores")
    public List<FornecedorResponseDTO> listarFornecedores(
            @RequestParam(required = false) Long empresaCompradoraId) {
        return marketplaceService.listarFornecedores(empresaCompradoraId);
    }

    @GetMapping("/fornecedores/{id}/produtos")
    public List<ProdutoResponseDTO> listarProdutosFornecedor(@PathVariable Long id) {
        return marketplaceService.listarProdutosFornecedor(id);
    }

    @GetMapping("/produtos-recentes")
    public List<ProdutoRecenteDTO> listarProdutosRecentes(
            @RequestParam(defaultValue = "20") int limite,
            @RequestAttribute(name = JwtAuthFilter.ATRIBUTO_USUARIO_ID, required = false) Long usuarioId) {
        return marketplaceService.listarProdutosRecentes(limite, marketplaceService.empresaDoUsuario(usuarioId));
    }

    @GetMapping("/quiosques")
    public List<QuiosqueDisponivelDTO> listarQuiosques(
            @RequestParam(required = false) String busca,
            @RequestAttribute(name = JwtAuthFilter.ATRIBUTO_USUARIO_ID, required = false) Long usuarioId) {
        return marketplaceService.listarQuiosquesDisponiveis(busca, marketplaceService.empresaDoUsuario(usuarioId));
    }
}
