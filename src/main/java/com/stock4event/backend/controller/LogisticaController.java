package com.stock4event.backend.controller;

import com.stock4event.backend.dto.LogisticaResumoDTO;
import com.stock4event.backend.service.LogisticaService;
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
