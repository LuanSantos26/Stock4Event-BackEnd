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

@RestController
@RequestMapping("/api/itens-pedido")
@CrossOrigin(origins = "*")
public class ItemPedidoController {

    @Autowired private ItemPedidoRepository repository;
    @Autowired private PedidoRepository     pedidoRepository;

    @GetMapping
    public List<ItemPedido> listar(@RequestParam(required = false) Long pedidoId) {
        if (pedidoId != null) return repository.findByPedidoId(pedidoId);
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemPedido> buscar(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ItemPedido> criar(@Valid @RequestBody ItemPedido item) {
        BigDecimal subtotal = item.getPrecoUnitario().multiply(item.getQuantidade());
        item.setSubtotal(subtotal);
        ItemPedido salvo = repository.save(item);

        recalcularTotal(salvo.getPedido().getId());

        return ResponseEntity.status(201).body(salvo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.findById(id).ifPresent(item -> {
            Long pedidoId = item.getPedido().getId();
            repository.deleteById(id);
            recalcularTotal(pedidoId);
        });
        return ResponseEntity.noContent().build();
    }

    private void recalcularTotal(Long pedidoId) {
        pedidoRepository.findById(pedidoId).ifPresent(pedido -> {
            BigDecimal total = repository.findByPedidoId(pedidoId)
                    .stream()
                    .map(ItemPedido::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            pedido.setValorTotal(total);
            pedidoRepository.save(pedido);
        });
    }
}
