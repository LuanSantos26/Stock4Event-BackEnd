package com.stock4event.backend.controller;

import com.stock4event.backend.config.JwtAuthFilter;
import com.stock4event.backend.dto.ProdutoPdvDTO;
import com.stock4event.backend.dto.VendaPdvRequestDTO;
import com.stock4event.backend.dto.VendaPdvResponseDTO;
import com.stock4event.backend.service.PdvService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Ponto de venda (PDV) dos quiosques. Usado pela tela src/screens/Pdv do app. */
@RestController
@RequestMapping("/api/pdv")
@CrossOrigin(origins = "*")
public class PdvController {

    @Autowired private PdvService pdvService;

    @GetMapping("/produtos")
    public List<ProdutoPdvDTO> listarProdutos(
            @RequestParam Long barracaId,
            @RequestParam(required = false) String busca,
            @RequestAttribute(name = JwtAuthFilter.ATRIBUTO_USUARIO_ID, required = false) Long usuarioId) {
        return pdvService.listarProdutos(barracaId, busca, usuarioId);
    }

    @PostMapping("/vendas")
    public ResponseEntity<VendaPdvResponseDTO> registrarVenda(
            @Valid @RequestBody VendaPdvRequestDTO dto,
            @RequestAttribute(name = JwtAuthFilter.ATRIBUTO_USUARIO_ID, required = false) Long usuarioId) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(pdvService.registrarVenda(dto, usuarioId));
        } catch (DataIntegrityViolationException e) {
            // Outra requisição com a mesma chave gravou primeiro: devolve a venda dela.
            return ResponseEntity.ok(pdvService.buscarPorChave(dto.getChaveVenda()));
        }
    }

    @GetMapping("/vendas/{id}")
    public VendaPdvResponseDTO buscarVenda(
            @PathVariable Long id,
            @RequestAttribute(name = JwtAuthFilter.ATRIBUTO_USUARIO_ID, required = false) Long usuarioId) {
        return pdvService.buscarVenda(id, usuarioId);
    }
}
