package com.quickstock.backend.dto;

import com.quickstock.backend.entity.CartaoPagamentoSalvo;
import lombok.Getter;

@Getter
public class CartaoPagamentoSalvoDTO {

    private final Long id;
    private final Long empresaId;
    private final String tipo;
    private final String apelido;
    private final String bandeira;
    private final String ultimosDigitos;
    private final String numeroMascarado;
    private final String validade;
    private final String titular;

    public CartaoPagamentoSalvoDTO(CartaoPagamentoSalvo cartao) {
        this.id = cartao.getId();
        this.empresaId = cartao.getEmpresa().getId();
        this.tipo = cartao.getTipo();
        this.apelido = cartao.getApelido();
        this.bandeira = cartao.getBandeira();
        this.ultimosDigitos = cartao.getUltimosDigitos();
        this.numeroMascarado = cartao.getNumeroMascarado();
        this.validade = cartao.getValidade();
        this.titular = cartao.getTitular();
    }
}
