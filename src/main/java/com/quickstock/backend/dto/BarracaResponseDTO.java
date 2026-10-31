package com.quickstock.backend.dto;

import com.quickstock.backend.entity.Barraca;
import com.quickstock.backend.entity.EstoqueBarraca;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Getter
public class BarracaResponseDTO {

    private final Long id;
    private final String nome;
    private final Long eventoId;
    private final String eventoNome;
    private final Integer ativa;
    private final int totalProdutos;
    private final BigDecimal totalUnidades;
    private final LocalDateTime atualizadoEm;
    private final List<EstoqueItemResponseDTO> itens;

    public BarracaResponseDTO(Barraca barraca, List<EstoqueBarraca> estoqueItens) {
        this.id = barraca.getId();
        this.nome = barraca.getNome();
        this.eventoId = barraca.getEvento().getId();
        this.eventoNome = barraca.getEvento().getNome();
        this.ativa = barraca.getAtiva();

        this.itens = estoqueItens.stream()
                .map(EstoqueItemResponseDTO::new)
                .toList();

        this.totalProdutos = (int) estoqueItens.stream()
                .filter(item -> item.getQuantidade().compareTo(BigDecimal.ZERO) > 0)
                .count();

        this.totalUnidades = estoqueItens.stream()
                .map(EstoqueBarraca::getQuantidade)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.atualizadoEm = estoqueItens.stream()
                .map(EstoqueBarraca::getAtualizadoEm)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }
}
