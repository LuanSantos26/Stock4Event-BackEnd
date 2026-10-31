package com.quickstock.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class SolicitacaoCompraRequestDTO {

    @NotNull
    private Long empresaCompradoraId;

    @NotNull
    private Long empresaFornecedoraId;

    @NotNull
    private Long usuarioId;

    @Size(max = 500)
    private String observacao;

    @NotBlank
    @Size(max = 20)
    private String metodoPagamento;

    private Long enderecoEntregaId;

    private BigDecimal taxaEntrega;

    @Size(max = 120)
    private String pagamentoReferencia;

    @Size(max = 300)
    private String pagamentoDetalhes;

    @Valid
    private List<ItemSolicitacaoRequestDTO> itens = new ArrayList<>();
}
