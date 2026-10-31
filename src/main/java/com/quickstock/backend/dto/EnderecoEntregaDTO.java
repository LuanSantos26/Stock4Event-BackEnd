package com.quickstock.backend.dto;

import com.quickstock.backend.entity.EnderecoEntrega;
import lombok.Getter;

@Getter
public class EnderecoEntregaDTO {

    private final Long id;
    private final Long empresaId;
    private final String apelido;
    private final String logradouro;
    private final String numero;
    private final String complemento;
    private final String bairro;
    private final String cidade;
    private final String uf;
    private final String cep;
    private final boolean principal;
    private final String resumo;

    public EnderecoEntregaDTO(EnderecoEntrega endereco) {
        this.id = endereco.getId();
        this.empresaId = endereco.getEmpresa().getId();
        this.apelido = endereco.getApelido();
        this.logradouro = endereco.getLogradouro();
        this.numero = endereco.getNumero();
        this.complemento = endereco.getComplemento();
        this.bairro = endereco.getBairro();
        this.cidade = endereco.getCidade();
        this.uf = endereco.getUf();
        this.cep = endereco.getCep();
        this.principal = endereco.isPrincipal();
        this.resumo = formatarResumo(endereco);
    }

    public static String formatarResumo(EnderecoEntrega endereco) {
        String base = endereco.getLogradouro() + ", " + endereco.getNumero();
        if (endereco.getComplemento() != null && !endereco.getComplemento().isBlank()) {
            base += " - " + endereco.getComplemento();
        }
        return base + " — " + endereco.getBairro() + ", " + endereco.getCidade() + "/" + endereco.getUf();
    }
}
