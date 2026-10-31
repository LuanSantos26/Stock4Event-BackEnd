package com.quickstock.backend.dto;

import lombok.Getter;

@Getter
public class CepConsultaDTO {

    private final String cep;
    private final String logradouro;
    private final String bairro;
    private final String cidade;
    private final String uf;

    public CepConsultaDTO(String cep, String logradouro, String bairro, String cidade, String uf) {
        this.cep = cep;
        this.logradouro = logradouro != null ? logradouro : "";
        this.bairro = bairro != null ? bairro : "";
        this.cidade = cidade != null ? cidade : "";
        this.uf = uf != null ? uf : "";
    }
}
