package com.quickstock.backend.controller;

import com.quickstock.backend.dto.LogisticaResumoDTO;
import com.quickstock.backend.service.LogisticaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logistica")
@CrossOrigin(origins = "*")
public class LogisticaController {

    @Autowired private LogisticaService logisticaService;

    @GetMapping
    public LogisticaResumoDTO resumo(@RequestParam Long empresaId) {
        return logisticaService.resumo(empresaId);
    }
}
