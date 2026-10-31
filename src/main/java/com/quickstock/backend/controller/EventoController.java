package com.quickstock.backend.controller;

import com.quickstock.backend.entity.Evento;
import com.quickstock.backend.repository.EventoRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/eventos")
@CrossOrigin(origins = "*")
public class EventoController {

    @Autowired private EventoRepository repository;

    @GetMapping
    public List<Evento> listar(@RequestParam(required = false) Long empresaId,
                                @RequestParam(required = false) String status) {
        if (empresaId != null && status != null) return repository.findByEmpresaIdAndStatus(empresaId, status);
        if (empresaId != null)                   return repository.findByEmpresaId(empresaId);
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Evento> buscar(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Evento> criar(@Valid @RequestBody Evento evento) {
        return ResponseEntity.status(201).body(repository.save(evento));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Evento> atualizar(@PathVariable Long id,
                                             @Valid @RequestBody Evento dados) {
        return repository.findById(id).map(ev -> {
            ev.setNome(dados.getNome());
            ev.setDataInicio(dados.getDataInicio());
            ev.setDataFim(dados.getDataFim());
            ev.setStatus(dados.getStatus());
            return ResponseEntity.ok(repository.save(ev));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
