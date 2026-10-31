package com.quickstock.backend.controller;

import com.quickstock.backend.entity.EstoqueBarraca;
import com.quickstock.backend.repository.EstoqueBarracaRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/estoque-barraca")
@CrossOrigin(origins = "*")
public class EstoqueBarracaController {

    @Autowired private EstoqueBarracaRepository repository;

    @GetMapping
    public List<EstoqueBarraca> listar(@RequestParam(required = false) Long barracaId) {
        if (barracaId != null) return repository.findByBarracaId(barracaId);
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EstoqueBarraca> buscar(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/barraca/{barracaId}/produto/{produtoId}")
    public ResponseEntity<EstoqueBarraca> buscarSaldo(@PathVariable Long barracaId,
                                                       @PathVariable Long produtoId) {
        return repository.findByBarracaIdAndProdutoId(barracaId, produtoId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<EstoqueBarraca> criar(@Valid @RequestBody EstoqueBarraca estoque) {
        return ResponseEntity.status(201).body(repository.save(estoque));
    }

    @PatchMapping("/{id}/quantidade")
    public ResponseEntity<EstoqueBarraca> ajustarQuantidade(@PathVariable Long id,
                                                             @RequestBody Map<String, Object> body) {
        return repository.findById(id).map(e -> {
            e.setQuantidade(new BigDecimal(body.get("quantidade").toString()));
            return ResponseEntity.ok(repository.save(e));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
