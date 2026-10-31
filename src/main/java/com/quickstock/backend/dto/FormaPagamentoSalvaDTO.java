package com.quickstock.backend.dto;

import com.quickstock.backend.entity.FormaPagamentoSalva;
import lombok.Getter;

@Getter
public class FormaPagamentoSalvaDTO {

    private final Long id;
    private final Long empresaId;
    private final String tipo;
    private final String apelido;
    private final String label;
    private final boolean principal;

    public FormaPagamentoSalvaDTO(FormaPagamentoSalva forma) {
        this.id = forma.getId();
        this.empresaId = forma.getEmpresa().getId();
        this.tipo = forma.getTipo();
        this.apelido = forma.getApelido();
        this.label = labelTipo(forma.getTipo());
        this.principal = forma.isPrincipal();
    }

    public static String labelTipo(String tipo) {
        if (tipo == null) return "Pagamento";
        return switch (tipo.toLowerCase()) {
            case "pix" -> "PIX";
            case "credito" -> "Crédito";
            case "debito" -> "Débito";
            case "dinheiro" -> "Dinheiro";
            default -> tipo;
        };
    }
}
