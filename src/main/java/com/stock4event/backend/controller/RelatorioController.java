package com.stock4event.backend.controller;

import com.stock4event.backend.dto.RelatorioDTO;
import com.stock4event.backend.service.RelatorioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/relatorios")
@CrossOrigin(origins = "*")
public class RelatorioController {

    @Autowired private RelatorioService relatorioService;

    /**
     * GET /api/relatorios?empresaId=1&perfil=fornecedor&periodo=mes&data=2026-11-18
     * perfil: fornecedor (vendas) ou cliente (compras). periodo: dia, mes ou ano.
     */
    @GetMapping
    public RelatorioDTO gerar(
            @RequestParam Long empresaId,
            @RequestParam(defaultValue = "cliente") String perfil,
            @RequestParam(defaultValue = "dia") String periodo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return relatorioService.gerar(empresaId, perfil, periodo, data);
    }
}
