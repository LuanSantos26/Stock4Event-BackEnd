package com.quickstock.backend.controller;

import com.quickstock.backend.dto.NotificacaoDTO;
import com.quickstock.backend.service.NotificacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificacoes")
@CrossOrigin(origins = "*")
public class NotificacaoController {

    @Autowired private NotificacaoService notificacaoService;

    @GetMapping
    public List<NotificacaoDTO> listar(@RequestParam Long empresaCompradoraId) {
        return notificacaoService.listar(empresaCompradoraId);
    }
}
