package com.stock4event.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Corpo de POST /api/pdv/vendas. Não traz preços nem total: o servidor calcula.
 * Formato enviado pelo front em src/types/pdv.ts (VendaPdvPayload).
 */
@Data
public class VendaPdvRequestDTO {

    @NotNull(message = "Informe o ponto de venda.")
    private Long barracaId;

    @NotBlank(message = "Chave da venda ausente.")
    @Size(max = 64, message = "Chave da venda inválida.")
    private String chaveVenda;

    @NotBlank(message = "Escolha a forma de pagamento.")
    private String formaPagamento;

    private BigDecimal valorRecebido;

    @Size(max = 120, message = "Nome do cliente deve ter no máximo 120 caracteres.")
    private String clienteNome;

    private String clienteDocumento;

    @NotEmpty(message = "Adicione ao menos um produto.")
    @Valid
    private List<Item> itens;

    @Data
    public static class Item {
        @NotNull(message = "Produto inválido.")
        private Long produtoId;

        @NotNull(message = "Quantidade inválida.")
        @DecimalMin(value = "0.001", message = "Quantidade deve ser maior que zero.")
        private BigDecimal quantidade;
    }
}
