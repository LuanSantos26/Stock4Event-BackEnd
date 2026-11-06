package com.stock4event.backend.controller;

import com.stock4event.backend.config.JwtAuthFilter;
import com.stock4event.backend.dto.DadosBancariosDTO;
import com.stock4event.backend.dto.DadosBancariosRequestDTO;
import com.stock4event.backend.service.DadosBancariosService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Conta de recebimento do fornecedor.
 * GET  /api/dados-bancarios?empresaId=  → 200 com os dados, ou 204 se ainda não cadastrou.
 * PUT  /api/dados-bancarios             → cria ou atualiza (só a própria empresa).
 */
@RestController
@RequestMapping("/api/dados-bancarios")
@CrossOrigin(origins = "*")
public class DadosBancariosController {

    @Autowired private DadosBancariosService service;

    @GetMapping
    public ResponseEntity<DadosBancariosDTO> buscar(@RequestParam Long empresaId) {
        return service.buscar(empresaId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping
    public DadosBancariosDTO salvar(
            @Valid @RequestBody DadosBancariosRequestDTO dto,
            @RequestAttribute(name = JwtAuthFilter.ATRIBUTO_USUARIO_ID, required = false) Long usuarioId) {
        return service.salvar(dto, usuarioId);
    }
}
