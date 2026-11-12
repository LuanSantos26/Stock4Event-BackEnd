package com.stock4event.backend.controller;

import com.stock4event.backend.dto.ChavePixSalvaDTO;
import com.stock4event.backend.dto.ChavePixSalvaRequestDTO;
import com.stock4event.backend.service.ChavePixSalvaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chaves-pix")
@CrossOrigin(origins = "*")
public class ChavePixSalvaController {

    @Autowired private ChavePixSalvaService chavePixSalvaService;

    @GetMapping
    public List<ChavePixSalvaDTO> listar(@RequestParam Long empresaId) {
        return chavePixSalvaService.listarPorEmpresa(empresaId);
    }

    @PostMapping
    public ResponseEntity<ChavePixSalvaDTO> criar(@Valid @RequestBody ChavePixSalvaRequestDTO dto) {
        return ResponseEntity.status(201).body(chavePixSalvaService.criar(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id, @RequestParam Long empresaId) {
        chavePixSalvaService.remover(id, empresaId);
        return ResponseEntity.noContent().build();
    }
}
