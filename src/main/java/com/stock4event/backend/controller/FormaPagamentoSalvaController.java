package com.stock4event.backend.controller;

import com.stock4event.backend.dto.FormaPagamentoSalvaDTO;
import com.stock4event.backend.dto.FormaPagamentoSalvaRequestDTO;
import com.stock4event.backend.service.FormaPagamentoSalvaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/formas-pagamento")
@CrossOrigin(origins = "*")
public class FormaPagamentoSalvaController {

    @Autowired private FormaPagamentoSalvaService formaPagamentoSalvaService;

    @GetMapping
    public List<FormaPagamentoSalvaDTO> listar(@RequestParam Long empresaId) {
        return formaPagamentoSalvaService.listarPorEmpresa(empresaId);
    }

    @PostMapping
    public ResponseEntity<FormaPagamentoSalvaDTO> criar(@Valid @RequestBody FormaPagamentoSalvaRequestDTO dto) {
        return ResponseEntity.status(201).body(formaPagamentoSalvaService.criar(dto));
    }

    @PatchMapping("/{id}/principal")
    public FormaPagamentoSalvaDTO definirPrincipal(@PathVariable Long id, @RequestParam Long empresaId) {
        return formaPagamentoSalvaService.definirPrincipal(id, empresaId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id, @RequestParam Long empresaId) {
        formaPagamentoSalvaService.remover(id, empresaId);
        return ResponseEntity.noContent().build();
    }
}
