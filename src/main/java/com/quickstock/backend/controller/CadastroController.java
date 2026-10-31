package com.quickstock.backend.controller;

import com.quickstock.backend.dto.CadastroContaRequestDTO;
import com.quickstock.backend.dto.UsuarioResponseDTO;
import com.quickstock.backend.service.CadastroService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cadastro")
@CrossOrigin(origins = "*")
public class CadastroController {

    @Autowired
    private CadastroService cadastroService;

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> cadastrar(@Valid @RequestBody CadastroContaRequestDTO request) {
        var usuario = cadastroService.cadastrarConta(request);
        return ResponseEntity.status(201).body(new UsuarioResponseDTO(usuario));
    }
}
