package com.quickstock.backend.controller;

import com.quickstock.backend.dto.BarracaEstoqueRequestDTO;
import com.quickstock.backend.dto.BarracaRequestDTO;
import com.quickstock.backend.dto.BarracaResponseDTO;
import com.quickstock.backend.service.BarracaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/barracas")
@CrossOrigin(origins = "*")
public class BarracaController {

    @Autowired private BarracaService barracaService;

    @GetMapping
    public List<BarracaResponseDTO> listar(@RequestParam(required = false) Long empresaId) {
        if (empresaId != null) {
            return barracaService.listarPorEmpresa(empresaId);
        }
        return List.of();
    }

    @GetMapping("/{id}")
    public ResponseEntity<BarracaResponseDTO> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(barracaService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<BarracaResponseDTO> criar(@Valid @RequestBody BarracaRequestDTO dto) {
        return ResponseEntity.status(201).body(barracaService.criar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BarracaResponseDTO> atualizar(@PathVariable Long id,
                                                         @Valid @RequestBody BarracaRequestDTO dto) {
        return ResponseEntity.ok(barracaService.atualizar(id, dto));
    }

    @PutMapping("/{id}/estoque")
    public ResponseEntity<BarracaResponseDTO> atualizarEstoque(@PathVariable Long id,
                                                                @Valid @RequestBody BarracaEstoqueRequestDTO dto) {
        return ResponseEntity.ok(barracaService.sincronizarEstoque(id, dto.getItens()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id,
                                          @RequestParam Long empresaId) {
        barracaService.desativar(id, empresaId);
        return ResponseEntity.noContent().build();
    }
}
