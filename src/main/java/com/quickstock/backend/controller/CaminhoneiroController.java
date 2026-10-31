package com.quickstock.backend.controller;

import com.quickstock.backend.dto.CaminhoneiroRequestDTO;
import com.quickstock.backend.dto.CaminhoneiroResponseDTO;
import com.quickstock.backend.service.LogisticaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/caminhoneiros")
@CrossOrigin(origins = "*")
public class CaminhoneiroController {

    @Autowired private LogisticaService logisticaService;

    @GetMapping
    public List<CaminhoneiroResponseDTO> listar(@RequestParam(required = false) Long empresaId) {
        return logisticaService.listar(empresaId);
    }

    @PostMapping
    public ResponseEntity<CaminhoneiroResponseDTO> criar(@Valid @RequestBody CaminhoneiroRequestDTO dto) {
        return ResponseEntity.status(201).body(logisticaService.criar(dto));
    }
}
