package com.quickstock.backend.controller;

import com.quickstock.backend.dto.CartaoPagamentoSalvoDTO;
import com.quickstock.backend.dto.CartaoPagamentoSalvoRequestDTO;
import com.quickstock.backend.service.CartaoPagamentoSalvoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cartoes-pagamento")
@CrossOrigin(origins = "*")
public class CartaoPagamentoSalvoController {

    @Autowired private CartaoPagamentoSalvoService cartaoPagamentoSalvoService;

    @GetMapping
    public List<CartaoPagamentoSalvoDTO> listar(
            @RequestParam Long empresaId,
            @RequestParam(required = false) String tipo) {
        return cartaoPagamentoSalvoService.listarPorEmpresa(empresaId, tipo);
    }

    @PostMapping
    public ResponseEntity<CartaoPagamentoSalvoDTO> criar(
            @Valid @RequestBody CartaoPagamentoSalvoRequestDTO dto) {
        return ResponseEntity.status(201).body(cartaoPagamentoSalvoService.criar(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id, @RequestParam Long empresaId) {
        cartaoPagamentoSalvoService.remover(id, empresaId);
        return ResponseEntity.noContent().build();
    }
}
