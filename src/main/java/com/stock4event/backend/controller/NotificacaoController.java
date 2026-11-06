package com.stock4event.backend.controller;

import com.stock4event.backend.dto.NotificacaoDTO;
import com.stock4event.backend.service.NotificacaoService;
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
