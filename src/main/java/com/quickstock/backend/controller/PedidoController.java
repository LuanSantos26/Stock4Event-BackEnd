package com.quickstock.backend.controller;

import com.quickstock.backend.entity.ItemPedido;
import com.quickstock.backend.entity.Pedido;
import com.quickstock.backend.repository.ItemPedidoRepository;
import com.quickstock.backend.repository.PedidoRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "*")
public class PedidoController {

    @Autowired private PedidoRepository     pedidoRepository;
    @Autowired private ItemPedidoRepository itemRepository;

    @GetMapping
    public List<Pedido> listar(@RequestParam(required = false) Long barracaId,
                                @RequestParam(required = false) String status) {
        if (barracaId != null && status != null) return pedidoRepository.findByBarracaIdAndStatus(barracaId, status);
        if (barracaId != null)                   return pedidoRepository.findByBarracaId(barracaId);
        return pedidoRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pedido> buscar(@PathVariable Long id) {
        return pedidoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/itens")
    public ResponseEntity<List<ItemPedido>> listarItens(@PathVariable Long id) {
        if (!pedidoRepository.existsById(id)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(itemRepository.findByPedidoId(id));
    }

    @PostMapping
    public ResponseEntity<Pedido> criar(@Valid @RequestBody Pedido pedido) {
        pedido.setStatus("aberto");
        pedido.setValorTotal(BigDecimal.ZERO);
        return ResponseEntity.status(201).body(pedidoRepository.save(pedido));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Pedido> atualizarStatus(@PathVariable Long id,
                                                   @RequestBody Map<String, String> body) {
        return pedidoRepository.findById(id).map(p -> {
            p.setStatus(body.get("status"));
            return ResponseEntity.ok(pedidoRepository.save(p));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Pedido> atualizar(@PathVariable Long id,
                                             @Valid @RequestBody Pedido dados) {
        return pedidoRepository.findById(id).map(p -> {
            p.setStatus(dados.getStatus());
            p.setValorTotal(dados.getValorTotal());
            return ResponseEntity.ok(pedidoRepository.save(p));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        if (!pedidoRepository.existsById(id)) return ResponseEntity.notFound().build();
        pedidoRepository.findById(id).ifPresent(p -> {
            p.setStatus("cancelado");
            pedidoRepository.save(p);
        });
        return ResponseEntity.noContent().build();
    }
}
