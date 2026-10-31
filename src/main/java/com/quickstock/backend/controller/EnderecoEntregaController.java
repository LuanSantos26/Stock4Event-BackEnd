package com.quickstock.backend.controller;

import com.quickstock.backend.dto.CepConsultaDTO;
import com.quickstock.backend.dto.EnderecoEntregaDTO;
import com.quickstock.backend.dto.EnderecoEntregaRequestDTO;
import com.quickstock.backend.service.CepConsultaService;
import com.quickstock.backend.service.EnderecoEntregaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enderecos")
@CrossOrigin(origins = "*")
public class EnderecoEntregaController {

    @Autowired private EnderecoEntregaService enderecoEntregaService;
    @Autowired private CepConsultaService cepConsultaService;

    @GetMapping
    public List<EnderecoEntregaDTO> listar(@RequestParam Long empresaId) {
        return enderecoEntregaService.listarPorEmpresa(empresaId);
    }

    @GetMapping("/cep/{cep}")
    public ResponseEntity<?> buscarCep(@PathVariable String cep) {
        return cepConsultaService.buscarPorCep(cep)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body(Map.of("erro", "CEP não encontrado.")));
    }

    @PostMapping
    public ResponseEntity<EnderecoEntregaDTO> criar(@Valid @RequestBody EnderecoEntregaRequestDTO dto) {
        return ResponseEntity.status(201).body(enderecoEntregaService.criar(dto));
    }

    @PutMapping("/{id}")
    public EnderecoEntregaDTO atualizar(@PathVariable Long id, @Valid @RequestBody EnderecoEntregaRequestDTO dto) {
        return enderecoEntregaService.atualizar(id, dto);
    }

    @PatchMapping("/{id}/principal")
    public EnderecoEntregaDTO definirPrincipal(@PathVariable Long id, @RequestParam Long empresaId) {
        return enderecoEntregaService.definirPrincipal(id, empresaId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id, @RequestParam Long empresaId) {
        enderecoEntregaService.remover(id, empresaId);
        return ResponseEntity.noContent().build();
    }
}
