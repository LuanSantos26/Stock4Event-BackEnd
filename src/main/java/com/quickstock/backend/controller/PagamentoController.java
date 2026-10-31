package com.quickstock.backend.controller;

import com.quickstock.backend.entity.Pagamento;
import com.quickstock.backend.repository.PagamentoRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pagamentos")
@CrossOrigin(origins = "*")
public class PagamentoController {

    @Autowired private PagamentoRepository repository;

    @GetMapping
    public List<Pagamento> listar(@RequestParam(required = false) Long pedidoId) {
        if (pedidoId != null) return repository.findByPedidoId(pedidoId);
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pagamento> buscar(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Pagamento> criar(@Valid @RequestBody Pagamento pagamento) {
        pagamento.setStatus("pendente");
        return ResponseEntity.status(201).body(repository.save(pagamento));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Pagamento> atualizarStatus(@PathVariable Long id,
                                                      @RequestBody Map<String, String> body) {
        return repository.findById(id).map(pg -> {
            pg.setStatus(body.get("status"));
            return ResponseEntity.ok(repository.save(pg));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
